# App 1 — Persistence

## Overview

App 1 writes to **two databases**:

| Store | What | Why |
|-------|------|-----|
| **PostgreSQL 16** | `transactions`, `accounts`, `ingested_files`, `transaction_validation_warnings` | Business data — queryable by App 2 |
| **MongoDB 7** | `file_uploads`, `processing_logs` | Audit log — unbounded growth, no relational queries |

---

## PostgreSQL — banking schema

### `ingested_files`

Written **once** at the start of processing (status=`RECEIVED`) and updated
at the end (status=`COMPLETED`/`FAILED`/`DUPLICATE`).

```sql
CREATE TABLE banking.ingested_files (
    id               UUID PRIMARY KEY,
    file_name        VARCHAR(255)  NOT NULL,
    file_format      VARCHAR(10)   NOT NULL,  -- TXT | CSV | JSON | XML
    status           VARCHAR(20)   NOT NULL,  -- RECEIVED | PROCESSING | COMPLETED | FAILED | DUPLICATE
    checksum         VARCHAR(64)   NOT NULL UNIQUE,  -- SHA-256 hex (V10 duplicate check)
    file_size_bytes  BIGINT,
    total_rows       INT,
    success_rows     INT,
    failed_rows      INT,
    uploaded_by      VARCHAR(30),
    uploaded_at      TIMESTAMPTZ,
    processed_at     TIMESTAMPTZ
);
```

> `checksum` has a UNIQUE constraint — used by V10.  
> Insert with `ON CONFLICT (checksum) DO NOTHING` + check rows affected to detect duplicates atomically.

---

### `accounts`

Accounts are **resolved or auto-created** during the ENRICH phase.
Use CBU as the lookup key (unique constraint). If an account with the given
CBU does not exist, create it from the DETAIL row fields.

```sql
CREATE TABLE banking.accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number   VARCHAR(20)   UNIQUE,  -- e.g. ACC-00123 (auto-generated if new)
    cbu              VARCHAR(22)   NOT NULL UNIQUE,
    cuit             VARCHAR(11)   NOT NULL UNIQUE,
    holder_name      VARCHAR(40)   NOT NULL,
    holder_type      VARCHAR(10),           -- INDIVIDUAL | COMPANY
    email            VARCHAR(100),
    phone            VARCHAR(30),
    bank_code        VARCHAR(20),           -- BIC / CBU prefix
    branch_code      VARCHAR(10),
    default_currency CHAR(3),
    is_active        BOOLEAN       NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

**Account resolution logic:**

```java
// In AccountResolutionService (domain)
public UUID resolveOrCreate(String cbu, String cuit, String holderName, ...) {
    return accountRepository.findByCbu(cbu)
        .map(Account::id)
        .orElseGet(() -> accountRepository.save(
            Account.newFrom(cbu, cuit, holderName, ...)
        ).id());
}
```

Use `INSERT ... ON CONFLICT (cbu) DO NOTHING RETURNING id` to avoid race
conditions when the same CBU appears in multiple concurrent files.

---

### `transactions`

Batch-inserted at chunk-size 500 using JDBC `batchUpdate`.

```sql
CREATE TABLE banking.transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_ref     VARCHAR(30)   NOT NULL UNIQUE,  -- from file, unique within file (V12)
    transaction_at   TIMESTAMPTZ   NOT NULL,
    ingested_at      TIMESTAMPTZ   NOT NULL,          -- = ingested_files.processed_at
    type             VARCHAR(10)   NOT NULL,
    status           VARCHAR(10)   NOT NULL,
    amount           NUMERIC(19,0) NOT NULL,           -- stored as cents (no division)
    currency         CHAR(3)       NOT NULL,
    benefactor_id    UUID          NOT NULL REFERENCES banking.accounts(id),
    beneficiary_id   UUID          NOT NULL REFERENCES banking.accounts(id),
    description      TEXT,
    file_id          UUID          NOT NULL REFERENCES banking.ingested_files(id),
    created_by       VARCHAR(30),
    flagged          BOOLEAN       NOT NULL DEFAULT false,
    flag_reason      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

> `amount` is stored as integer cents in a `NUMERIC(19,0)` column.  
> **Never divide by 100 before inserting.** Division happens in App 2's serializer.

**JDBC batch insert:**

```java
jdbcTemplate.batchUpdate(
    """
    INSERT INTO banking.transactions
        (id, external_ref, transaction_at, ingested_at, type, status,
         amount, currency, benefactor_id, beneficiary_id, description,
         file_id, created_by, flagged)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    ON CONFLICT (external_ref) DO NOTHING
    """,
    chunk,   // List<Transaction>, max 500
    chunk.size(),
    (ps, tx) -> { /* set parameters */ }
);
```

---

### `transaction_validation_warnings`

Written for every row-level violation (V11–V24) and for `MISSING_DESCRIPTION`.
Written outside the transaction batch — each warning is persisted regardless
of whether the row was skipped or accepted.

```sql
CREATE TABLE banking.transaction_validation_warnings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id   UUID          REFERENCES banking.transactions(id),  -- nullable if row was skipped
    warning_code     VARCHAR(50)   NOT NULL,
    warning_message  TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

> `transaction_id` is **nullable**: if the row was skipped (hard validation
> failure), there is no transaction to reference.

---

## MongoDB — audit collections

### `file_uploads`

One document per file. Mirrors `ingested_files` with the full processing result.
`_id` = same UUID as `ingested_files.id`.

```json
{
  "_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fileName": "transaction_example_20240315.txt",
  "fileFormat": "TXT",
  "status": "COMPLETED",
  "checksum": "a3f5c9d1...",
  "fileSizeBytes": 204800,
  "totalRows": 3,
  "successRows": 2,
  "failedRows": 1,
  "flaggedRows": 1,
  "branchCode": "BR-042",
  "uploadedBy": "operator-42",
  "uploadedAt": "2024-03-15T09:05:00Z",
  "processedAt": "2024-03-15T09:05:22Z",
  "errorMessage": null,
  "headerRecord": { ... },
  "trailerRecord": { ... }
}
```

### `processing_logs`

One document per file, containing an array of log entries.
`_id` = same UUID as `ingested_files.id`.

```json
{
  "_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "entries": [
    {
      "timestamp": "2024-03-15T09:05:10Z",
      "level": "INFO",
      "phase": "PARSE",
      "message": "Header parsed successfully",
      "lineNumber": null,
      "data": {}
    },
    {
      "timestamp": "2024-03-15T09:05:11Z",
      "level": "ERROR",
      "phase": "VALIDATE",
      "message": "Row skipped: benefactor_cbu must be exactly 22 digits (V19)",
      "lineNumber": 3,
      "data": {
        "field": "benefactor_cbu",
        "rejectedValue": "0000031000123",
        "validationRule": "V19",
        "externalRef": "TNX-2024-000003"
      }
    }
  ]
}
```

**Write strategy:** accumulate log entries in memory during processing,
then `$push` them in a single `updateOne` with `upsert=true` at the end.
Do not write one entry at a time — batch the entire run's log.

---

## Transaction ordering (write sequence)

```
1. INSERT ingested_files (status=RECEIVED)
2. For each chunk of 500 valid rows:
   a. Resolve/create accounts (ENRICH)
   b. JDBC batchUpdate → transactions
   c. Save validation_warnings for the chunk
3. UPDATE ingested_files (status=COMPLETED/FAILED, success_rows, failed_rows, processed_at)
4. MongoDB updateOne file_uploads (upsert)
5. MongoDB updateOne processing_logs (upsert, push all entries)
6. Publish FileProcessedEvent → Kafka
7. ACK Kafka offset
```

Steps 1–5 must be completed before publishing the Kafka event.  
If any step 1–3 fails with a DB error, do **not** ack the Kafka offset.
