# Banking Transaction Platform — Project Context

## What is this project?

A **banking transaction ingestion and query platform** composed of three services:

| Service | Role |
|---------|------|
| **App 1 — Ingestion Service** | Kafka consumer. Reads, validates, and persists transaction files. |
| **App 2 — Query API** | Read-only REST API over the persisted data. |
| **App 3 — Report Service** | Consumes `transaction.file.processed` events, generates reports using App 2. |

**You are working on App 1.**  
See `app1/` for all App 1 specifications. Start there.

---

## Repository layout (target)

```
banking-transaction-platform/
├── CLAUDE.md                        ← this file
├── docs/
│   ├── app1/
│   │   ├── 00-overview.md           ← start here for App 1
│   │   ├── 01-architecture.md       ← hexagonal architecture, package layout
│   │   ├── 02-kafka.md              ← inbound/outbound Kafka contracts
│   │   ├── 03-file-format.md        ← TXT fixed-width spec (all record types)
│   │   ├── 04-validation.md         ← V01–V29 rules, error handling strategy
│   │   ├── 05-persistence.md        ← PostgreSQL + MongoDB schemas and write strategy
│   │   ├── 06-openapi.md            ← internal admin REST API
│   │   └── 07-testing.md            ← testing requirements and conventions
│   └── shared/
│       ├── db-schema.md             ← PostgreSQL ERD (all tables)
│       └── glossary.md              ← CBU, CUIT, amount_cents, external_ref …
```

---

## Tech stack (App 1)

```
Java 17
Spring Boot 3.3.x
Spring Kafka 3.x          — @KafkaListener, MANUAL_IMMEDIATE ack
Spring Batch              — FlatFileItemReader, FixedLengthTokenizer, chunk=500
Hibernate 6 / JDBC        — PostgreSQL writes
Spring Data MongoDB        — audit log writes
PostgreSQL 16             — banking schema
MongoDB 7                 — file_uploads, processing_logs
Apache Kafka 3.7          — consumer group: ingestion-svc
Maven
```

---

## Non-negotiable constraints

- **Domain package has zero Spring / JDBC / Kafka imports.** All framework coupling lives in adapters.
- **Amounts are always integer cents** in the file and in the DB. Divide by 100 only at the API response layer.
- **SHA-256 duplicate check** runs before any parsing. A duplicate file must be rejected immediately (status `DUPLICATE`) without touching the DB.
- **Chunk size = 500 rows** per Spring Batch commit. Do not change without updating the spec.
- **Ack mode = MANUAL\_IMMEDIATE.** Kafka offset is committed only after the file is fully processed and the `FileProcessedEvent` is published. On DB error, do NOT ack — let Kafka redeliver.
- **80 % test coverage** is a hard requirement (unit + integration).
- **All timestamps are UTC.** No timezone conversion anywhere in the service.

---

## Kafka topics

| Topic | Direction | Key |
|-------|-----------|-----|
| `transaction.file.ready` | Inbound | — |
| `transaction.file.processed` | Outbound | `fileId` (UUID string) |

Producer: `acks=all`, idempotent, 3 retries.

---

## Where to go next

Read **`docs/app1/00-overview.md`** for the full processing flow, then follow the numbered files in order.
