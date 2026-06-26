# App 1 — Ingestion Service: Overview

## Purpose

App 1 is the **entry point** of the banking transaction platform.  
It has **no public REST endpoints**. Its only entry point is a Kafka topic.

---

## Processing flow

```
[Kafka] transaction.file.ready
        │
        ▼
  1. Idempotency check       — reject if eventId already processed
        │
        ▼
  2. Duplicate file check    — SHA-256 vs ingested_files.checksum
        │  duplicate → status=DUPLICATE, publish event, ACK
        ▼
  3. Parse file              — Strategy: TXT / CSV / JSON / XML
        │  unsupported format → status=FAILED, publish event, ACK
        ▼
  4. File-level validation   — V01–V10 (fail entire file on any violation)
        │  failure → status=FAILED, publish event, ACK
        ▼
  5. Row-level processing    — for each DETAIL row:
        │   a. Validate row  — V11–V24 (skip row on failure, log warning)
        │   b. Enrich        — resolve / auto-create accounts
        │   c. Persist row   — batch insert to PostgreSQL (chunk=500)
        │   d. Log           — write entry to MongoDB processing_logs
        ▼
  6. Reconciliation checks   — V25–V29 (log warnings, do not fail file)
        │
        ▼
  7. Write file metadata     — PostgreSQL ingested_files + MongoDB file_uploads
        │
        ▼
  8. Publish FileProcessedEvent — topic: transaction.file.processed
        │
        ▼
  9. ACK Kafka offset        — MANUAL_IMMEDIATE, only reached here
```

> **On DB connection error:** do NOT ack. Kafka redelivers after restart.  
> **On Kafka publish failure:** log error. File is persisted — downstream may miss the event.

---

## Error handling matrix

| Scenario | File status | Kafka ACK | Notes |
|----------|-------------|-----------|-------|
| Unsupported file format | `FAILED` | Yes | Publish FAILED event |
| Duplicate file (V10) | `DUPLICATE` | Yes | Publish DUPLICATE event |
| File-level validation failure (V01–V09) | `FAILED` | Yes | Publish FAILED event |
| Row failures < 100 % | `COMPLETED` | Yes | `failedRows > 0`, valid rows persisted |
| All rows fail | `FAILED` | Yes | No transactions written |
| DB connection error | — | **No** | Kafka redelivers |
| Kafka producer failure | `COMPLETED`/`FAILED` | Yes | Log error, continue |

---

## Interfaces summary

### Inbound
- **Kafka topic** `transaction.file.ready` (group `ingestion-svc`)  
  Schema: `FileReadyEvent` — see `02-kafka.md`

### Outbound
- **Kafka topic** `transaction.file.processed`  
  Schema: `FileProcessedEvent` — see `02-kafka.md`
- **PostgreSQL** — `transactions`, `accounts`, `ingested_files`, `transaction_validation_warnings`  
  See `05-persistence.md` and `docs/shared/db-schema.md`
- **MongoDB** — `file_uploads`, `processing_logs`  
  See `05-persistence.md`
- **Internal REST API** (admin/monitoring only)  
  See `06-openapi.md`

---

## Key invariants

1. `transactions.ingested_at` = `ingested_files.processed_at` for every transaction in the file.
2. `transactions.created_by` = `FileReadyEvent.uploadedBy`.
3. `transactions.file_id` = `ingested_files.id` = MongoDB `_id`.
4. A file ID is assigned once (from the HEADER record for TXT, or generated at intake for other formats) and never changes.
5. `amount` in the DB is stored as `numeric` (no cents division). Division by 100 happens **only** in API response serializers.
