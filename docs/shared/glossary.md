# Shared — Glossary

## Domain terms

| Term | Definition |
|------|-----------|
| **CBU** | *Clave Bancaria Uniforme* — 22-digit Argentine bank account identifier. Every account has a unique CBU. Used as the primary lookup key for account resolution in App 1. |
| **CUIT** | *Código Único de Identificación Tributaria* — 11-digit Argentine tax identifier. Tied to a person or company. Used alongside CBU to identify account holders. |
| **amount_cents** | Amounts in the TXT file and in the PostgreSQL `transactions.amount` column are stored as **integer cents** with no decimal point. `$1,250.75` = `125075`. Division by 100 happens only at the API response layer (App 2). |
| **external_ref** | The bank's own reference for a transaction, present in the TXT DETAIL record (pos 2–31, max 30 chars). Must be unique within a file (V12). Maps to `transactions.external_ref`. |
| **benefactor** | The **sender** (source) of funds in a transaction. Maps to `transactions.benefactor_id` → `accounts`. |
| **beneficiary** | The **receiver** (destination) of funds. Maps to `transactions.beneficiary_id` → `accounts`. |
| **file_id** | UUID that identifies a file throughout its lifecycle. For TXT files, read from `HEADER.file_id` (pos 2–37). Shared across PostgreSQL (`ingested_files.id`) and MongoDB (`file_uploads._id`, `processing_logs._id`). |
| **ingested_at** | UTC timestamp when App 1 finished processing the file. Set on `ingested_files.processed_at` and propagated to every `transactions.ingested_at` row from that file. |
| **transaction_at** | UTC timestamp when the real-world transaction occurred, as declared in the file (`DETAIL.transaction_at`). Independent from `ingested_at`. |
| **chunk** | Spring Batch unit of commit. Default = 500 rows. One DB transaction per chunk. |
| **FileReadyEvent** | Inbound Kafka message — signals that a file is ready to be consumed. Produced by the upstream file-upload service on topic `transaction.file.ready`. |
| **FileProcessedEvent** | Outbound Kafka message — published by App 1 after every ingestion attempt. Consumed by App 2 and App 3. Topic: `transaction.file.processed`. |

## File format terms

| Term | Definition |
|------|-----------|
| **HEADER record** | First line of every TXT file. `rec_type = H`. Contains file-level metadata: UUID, version, origin, date range, checksum. |
| **DETAIL record** | One line per transaction. `rec_type = D`. Contains all transaction fields. |
| **TRAILER record** | Last line of every TXT file. `rec_type = T`. Contains totals used for reconciliation (V25–V29). |
| **period_from / period_to** | Date range declared in HEADER and TRAILER. All `transaction_at` dates in DETAIL rows must fall within this range (V14). |
| **checksum** | SHA-256 hex of all raw DETAIL lines concatenated (before field parsing). Declared in HEADER (pos 142–205), validated by App 1 (V08). Also used for duplicate file detection (V10). |

## Validation rule shorthand

| Prefix | Meaning |
|--------|---------|
| **V01–V10** | File-level rules — failure rejects the entire file |
| **V11–V24** | Row-level rules — failure skips the row, processing continues |
| **V25–V29** | Reconciliation rules — failure is logged as a warning only |

## Status values

### File status (`ingested_files.status`)

| Value | Meaning |
|-------|---------|
| `RECEIVED` | Kafka event consumed, processing not yet started |
| `PROCESSING` | Currently being parsed/validated/persisted |
| `COMPLETED` | Processing finished — may have `failed_rows > 0` |
| `FAILED` | File-level validation failed or all rows rejected |
| `DUPLICATE` | SHA-256 matches a previously ingested file |

### Transaction status (`transactions.status`)

Values come directly from the DETAIL record field. App 1 does not derive or change them.

| Value | Meaning |
|-------|---------|
| `PENDING` | Transaction declared but not yet settled |
| `COMPLETED` | Transaction settled |
| `FAILED` | Transaction failed at the source system |
| `REVERSED` | Transaction was reversed |

### Transaction type (`transactions.type`)

| Value | Meaning |
|-------|---------|
| `DEBIT` | Funds withdrawn from benefactor |
| `CREDIT` | Funds deposited to beneficiary |
| `TRANSFER` | Internal transfer between accounts |
| `PAYMENT` | Bill or service payment |
| `REFUND` | Refund of a previous transaction |
| `FEE` | Bank fee |
