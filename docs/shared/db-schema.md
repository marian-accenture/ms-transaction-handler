# Shared — PostgreSQL Schema (banking)

## Tables

```
banking.accounts
banking.ingested_files
banking.transactions
banking.transaction_validation_warnings
```

## ERD (relationships)

```
accounts ─────────────────────────────────────┐
  (id)                                         │ benefactor_id
         ┌───────────────────────────────────► transactions ◄──── ingested_files
accounts ─┘                                        │ (id)              (id)
  (id)     beneficiary_id                          │
                                                   ▼
                                    transaction_validation_warnings
                                          (transaction_id FK, nullable)
```

## `accounts`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | `gen_random_uuid()` |
| `account_number` | VARCHAR(20) | UNIQUE | Auto-generated e.g. `ACC-00123` |
| `cbu` | VARCHAR(22) | NOT NULL, UNIQUE | 22-digit Argentine bank ID |
| `cuit` | VARCHAR(11) | NOT NULL, UNIQUE | 11-digit Argentine tax ID |
| `holder_name` | VARCHAR(40) | NOT NULL | From file field (max 40 chars) |
| `holder_type` | VARCHAR(10) | | `INDIVIDUAL` or `COMPANY` |
| `email` | VARCHAR(100) | | |
| `phone` | VARCHAR(30) | | |
| `bank_code` | VARCHAR(20) | | BIC or CBU prefix |
| `branch_code` | VARCHAR(10) | | |
| `default_currency` | CHAR(3) | | ISO 4217 |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

## `ingested_files`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | Shared with MongoDB `_id` |
| `file_name` | VARCHAR(255) | NOT NULL | |
| `file_format` | VARCHAR(10) | NOT NULL | `TXT` \| `CSV` \| `JSON` \| `XML` |
| `status` | VARCHAR(20) | NOT NULL | `RECEIVED` \| `PROCESSING` \| `COMPLETED` \| `FAILED` \| `DUPLICATE` |
| `checksum` | VARCHAR(64) | NOT NULL, UNIQUE | SHA-256 hex — V10 duplicate check |
| `file_size_bytes` | BIGINT | | |
| `total_rows` | INT | | From TRAILER.total_detail_lines |
| `success_rows` | INT | | Rows persisted |
| `failed_rows` | INT | | Rows skipped |
| `uploaded_by` | VARCHAR(30) | | From FileReadyEvent |
| `uploaded_at` | TIMESTAMPTZ | | When Kafka event was consumed |
| `processed_at` | TIMESTAMPTZ | | When processing completed → `transactions.ingested_at` |

## `transactions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | `gen_random_uuid()` |
| `external_ref` | VARCHAR(30) | NOT NULL, UNIQUE | From file — V12 |
| `transaction_at` | TIMESTAMPTZ | NOT NULL | From DETAIL.transaction_at (UTC) |
| `ingested_at` | TIMESTAMPTZ | NOT NULL | = `ingested_files.processed_at` |
| `type` | VARCHAR(10) | NOT NULL | `DEBIT` \| `CREDIT` \| `TRANSFER` \| `PAYMENT` \| `REFUND` \| `FEE` |
| `status` | VARCHAR(10) | NOT NULL | `PENDING` \| `COMPLETED` \| `FAILED` \| `REVERSED` |
| `amount` | NUMERIC(19,0) | NOT NULL | **Integer cents** — do NOT divide before storing |
| `currency` | CHAR(3) | NOT NULL | ISO 4217 |
| `benefactor_id` | UUID | NOT NULL, FK → accounts | Sender |
| `beneficiary_id` | UUID | NOT NULL, FK → accounts | Receiver |
| `description` | TEXT | | From DETAIL.description (max 50 chars) |
| `file_id` | UUID | NOT NULL, FK → ingested_files | |
| `created_by` | VARCHAR(30) | | = `FileReadyEvent.uploadedBy` |
| `flagged` | BOOLEAN | NOT NULL DEFAULT false | From DETAIL.flagged |
| `flag_reason` | TEXT | | Set by enrichment (post-ingestion) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

## `transaction_validation_warnings`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | `gen_random_uuid()` |
| `transaction_id` | UUID | FK → transactions, **nullable** | Null when row was skipped |
| `warning_code` | VARCHAR(50) | NOT NULL | Machine-readable e.g. `INVALID_BENEFACTOR_CBU` |
| `warning_message` | TEXT | | Human-readable detail |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

## Key indexes (App 1 writes, App 2 reads)

```sql
-- App 2 search by CBU (bilateral: benefactor OR beneficiary)
CREATE INDEX idx_tx_benefactor_id   ON banking.transactions(benefactor_id);
CREATE INDEX idx_tx_beneficiary_id  ON banking.transactions(beneficiary_id);
CREATE INDEX idx_accounts_cbu       ON banking.accounts(cbu);
CREATE INDEX idx_accounts_cuit      ON banking.accounts(cuit);

-- App 2 date range queries
CREATE INDEX idx_tx_transaction_at  ON banking.transactions(transaction_at);
CREATE INDEX idx_tx_ingested_at     ON banking.transactions(ingested_at);

-- App 2 file filter
CREATE INDEX idx_tx_file_id         ON banking.transactions(file_id);

-- Flagged filter
CREATE INDEX idx_tx_flagged         ON banking.transactions(flagged) WHERE flagged = true;
```
