# App 1 — TXT File Format

## Global rules

| Parameter | Value |
|-----------|-------|
| Encoding | UTF-8 |
| Line terminator | LF (`\n`) — reject files with CRLF |
| Record length | **300 characters** (all record types) |
| Decimal separator | `.` (period) |
| Thousands separator | None |
| Date format | `YYYYMMDD` |
| Timestamp format | `YYYYMMDDHHMMSS` (UTC) |
| Alphanumeric alignment | Left-aligned, space-padded right |
| Numeric alignment | Right-aligned, zero-padded left |
| Amount storage | Integer cents — `$1,250.75` → `000000000125075` |
| Null representation | All spaces in the field |

File structure (always in this order):

```
Line 1        → HEADER   (rec_type = H)
Lines 2…N-1   → DETAIL   (rec_type = D), one per transaction
Line N        → TRAILER  (rec_type = T)
```

---

## HEADER record (rec_type = H)

> The header record is a **data record** — `skip-lines = 0` in Spring Batch.

| Field | Pos | Len | Type | Description |
|-------|-----|-----|------|-------------|
| `rec_type` | 1 | 1 | A | Always `H` |
| `file_id` | 2–37 | 36 | AN | UUID → `ingested_files.id` |
| `file_version` | 38–41 | 4 | N | Must be `0100` (V09) |
| `origin_system` | 42–61 | 20 | A | System that generated the file |
| `branch_code` | 62–71 | 10 | AN | E.g. `BR-042` |
| `generated_by` | 72–101 | 30 | AN | Operator/service → `uploaded_by` |
| `generated_at` | 102–115 | 14 | N | `YYYYMMDDHHMMSS` UTC |
| `period_from` | 116–123 | 8 | N | Start of tx date range `YYYYMMDD` |
| `period_to` | 124–131 | 8 | N | End of tx date range `YYYYMMDD` |
| `currency` | 132–134 | 3 | A | Primary ISO 4217 code |
| `total_records` | 135–141 | 7 | N | Count of DETAIL records (V04) |
| `checksum` | 142–205 | 64 | AN | SHA-256 hex of all DETAIL lines (V08) |
| *(padding)* | 206–300 | 95 | A | Spaces |

> **Checksum note:** SHA-256 produces 64 hex chars (pos 142–205). The record
> is padded to 300 chars. Compute the hash over all raw DETAIL lines
> concatenated (before any field parsing) and compare with this value (V08).

---

## DETAIL record (rec_type = D)

One line per transaction. Lines are ordered by `transaction_at` ascending.

| Field | Pos | Len | Type | Description |
|-------|-----|-----|------|-------------|
| `rec_type` | 1 | 1 | A | Always `D` |
| `external_ref` | 2–31 | 30 | AN | Bank reference — unique within file (V12) |
| `transaction_at` | 32–45 | 14 | N | `YYYYMMDDHHMMSS` UTC (V13, V14) |
| `type` | 46–53 | 8 | A | `DEBIT`, `CREDIT`, `TRANSFER`, `PAYMENT`, `REFUND`, `FEE` — space-padded (V15) |
| `status` | 54–62 | 9 | A | `PENDING`, `COMPLETED`, `FAILED`, `REVERSED` — space-padded (V16) |
| `amount_cents` | 63–77 | 15 | N | Integer cents, zero-padded. Must be ≥ 0 (V17) |
| `currency` | 78–80 | 3 | A | ISO 4217 (V18) |
| `benefactor_cbu` | 81–102 | 22 | N | Sender CBU — exactly 22 digits (V19) |
| `benefactor_cuit` | 103–113 | 11 | N | Sender CUIT — exactly 11 digits (V20) |
| `benefactor_name` | 114–153 | 40 | AN | Sender full name |
| `beneficiary_cbu` | 154–175 | 22 | N | Receiver CBU — exactly 22 digits (V21) |
| `beneficiary_cuit` | 176–186 | 11 | N | Receiver CUIT — exactly 11 digits (V22) |
| `beneficiary_name` | 187–226 | 40 | AN | Receiver full name |
| `description` | 227–276 | 50 | AN | Free text — optional (empty → MISSING_DESCRIPTION warning) |
| `flagged` | 277 | 1 | A | `Y` or `N` (V24) |
| `reserved` | 278–300 | 23 | A | Spaces — reserved for future use |

**Field types:**
- `A` = Alphabetic only (A–Z, space)
- `N` = Numeric only (0–9)
- `AN` = Alphanumeric (printable ASCII, no pipe or newline)

---

## TRAILER record (rec_type = T)

Always the last line. Used for integrity reconciliation (V04–V07, V25–V29).

| Field | Pos | Len | Type | Description |
|-------|-----|-----|------|-------------|
| `rec_type` | 1 | 1 | A | Always `T` |
| `total_detail_lines` | 2–8 | 7 | N | Count of D records — must match `HEADER.total_records` (V04) |
| `total_amount_cents` | 9–27 | 19 | N | Sum of all `amount_cents` (V25) |
| `total_debit_cents` | 28–46 | 19 | N | Sum where `type=DEBIT` (V26) |
| `total_credit_cents` | 47–65 | 19 | N | Sum where `type=CREDIT` (V27) |
| `total_transfer_cents` | 66–84 | 19 | N | Sum where `type=TRANSFER` (V28) |
| `total_flagged` | 85–88 | 4 | N | Count where `flagged=Y` (V29) |
| `period_from` | 89–96 | 8 | N | Must match `HEADER.period_from` (V06) |
| `period_to` | 97–104 | 8 | N | Must match `HEADER.period_to` (V07) |
| `file_id` | 105–140 | 36 | AN | Must match `HEADER.file_id` (V05) |
| `reserved` | 141–300 | 160 | A | Spaces |

---

## Complete file example

```
H3fa85f64-5717-4562-b3fc-2c963f66afa601  00BRANCH_TERMINAL    BR-042    svc-ingestion                 202403150905222024030120240331USD0000003a3f5c9d1e2b4f6a8c0e1d3b5f7a9c2e4d6f8b0a2c4e6f8a0b2d4f6a8c0e2d4f6a8                                                                               
DTNX-2024-000001              20240315090000DEBIT    COMPLETED000000000025000USD0000003100012345678901203012345671Alice Martinez                          0000009900098765432101209876543211ACME Corp                              ATM withdrawal                                    N                       
DTNX-2024-000002              20240315093000TRANSFER COMPLETED000000000500000USD0000003100012345678901203012345671Alice Martinez                          0000004500011122233301204511122231Bob Fernandez                           Monthly rent payment                              N                       
DTNX-2024-000003              20240315143000PAYMENT  PENDING  000000008975000USD0000006700033344455501603033444551Carol Rodriguez                         0000009900099887766101209988776611EXTERNAL WIRE                           International wire transfer               Y                       
T0000003000000000950000000000000000250000000000000005000000000000000089750000000220240301202403313fa85f64-5717-4562-b3fc-2c963f66afa6                                                                                                                                                        
```

---

## Amount conversion

```java
// Parsing: file → domain
BigDecimal amount = new BigDecimal(amountCentsString.trim())
    .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);

// DB storage: store the original long value (amount_cents) in the numeric column
// API response: divide by 100 at the serializer layer only
```

> **Do not** store divided amounts in the DB. The `transactions.amount` column
> stores cents as a `numeric` without scale — e.g. `25000` for $250.00.
> App 2 divides by 100 in its response serializer.
