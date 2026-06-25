# App 1 — Validation Rules

## Overview

Validation is split into three layers:

| Layer | Rules | On failure |
|-------|-------|------------|
| File-level | V01–V10 | Reject entire file (`status=FAILED`) |
| Row-level | V11–V24 | Skip row, log warning, continue |
| Reconciliation | V25–V29 | Log warning, **do not** fail the file |

---

## File-level checks (V01–V10)

Evaluated before any row processing. Any single failure = file `FAILED`.

| Rule | Check |
|------|-------|
| **V01** | First line `rec_type` must be `H` |
| **V02** | Last line `rec_type` must be `T` |
| **V03** | No lines between HEADER and TRAILER may have `rec_type = H` or `T` |
| **V04** | `TRAILER.total_detail_lines` must equal the count of `D` lines in the file |
| **V05** | `TRAILER.file_id` must equal `HEADER.file_id` |
| **V06** | `TRAILER.period_from` must equal `HEADER.period_from` |
| **V07** | `TRAILER.period_to` must equal `HEADER.period_to` |
| **V08** | SHA-256 of all DETAIL lines concatenated must match `HEADER.checksum`. Compute before parsing individual fields. |
| **V09** | `HEADER.file_version` must be `0100`. Reject unknown versions. |
| **V10** | File checksum must not already exist in `ingested_files.checksum` (duplicate detection). |

> **V08 implementation note:** concatenate the raw bytes of every DETAIL line
> (including the trailing LF) before tokenizing fields. Compare the computed
> hash against `HEADER.checksum`. Fail immediately if they differ.

---

## Row-level checks (V11–V24)

Evaluated per DETAIL row. On failure: skip row, write to
`transaction_validation_warnings`, write ERROR entry to MongoDB
`processing_logs`, increment `failedRows`. Continue with next row.

| Rule | Field | Check |
|------|-------|-------|
| **V11** | `rec_type` | Must be exactly `D` |
| **V12** | `external_ref` | Must be non-empty and unique within the file |
| **V13** | `transaction_at` | Must be a valid `YYYYMMDDHHMMSS` timestamp and not in the future |
| **V14** | `transaction_at` | Date part must fall within `HEADER.period_from` – `HEADER.period_to` (inclusive) |
| **V15** | `type` | Must be one of: `DEBIT`, `CREDIT`, `TRANSFER`, `PAYMENT`, `REFUND`, `FEE` |
| **V16** | `status` | Must be one of: `PENDING`, `COMPLETED`, `FAILED`, `REVERSED` |
| **V17** | `amount_cents` | Must be numeric and ≥ 0 |
| **V18** | `currency` | Must be a valid ISO 4217 3-letter uppercase code |
| **V19** | `benefactor_cbu` | Must be exactly 22 digits (`\d{22}`) |
| **V20** | `benefactor_cuit` | Must be exactly 11 digits (`\d{11}`) |
| **V21** | `beneficiary_cbu` | Must be exactly 22 digits (`\d{22}`) |
| **V22** | `beneficiary_cuit` | Must be exactly 11 digits (`\d{11}`) |
| **V23** | `benefactor_cbu` vs `beneficiary_cbu` | Must not be equal (no self-transfers) |
| **V24** | `flagged` | Must be `Y` or `N` |

### Warning codes (for `transaction_validation_warnings.warning_code`)

Use machine-readable codes. Examples:

| Warning code | Triggered by |
|--------------|-------------|
| `INVALID_EXTERNAL_REF` | V12 — empty or duplicate |
| `INVALID_TRANSACTION_DATE` | V13 |
| `DATE_OUT_OF_PERIOD` | V14 |
| `INVALID_TYPE` | V15 |
| `INVALID_STATUS` | V16 |
| `INVALID_AMOUNT` | V17 |
| `INVALID_CURRENCY` | V18 |
| `INVALID_BENEFACTOR_CBU` | V19 |
| `INVALID_BENEFACTOR_CUIT` | V20 |
| `INVALID_BENEFICIARY_CBU` | V21 |
| `INVALID_BENEFICIARY_CUIT` | V22 |
| `SELF_TRANSFER` | V23 |
| `INVALID_FLAGGED_VALUE` | V24 |
| `MISSING_DESCRIPTION` | `description` field is all spaces (non-fatal warning, row is still persisted) |

> `MISSING_DESCRIPTION` is the only warning that does **not** cause the row
> to be skipped. All V11–V24 failures skip the row.

---

## Reconciliation checks (V25–V29)

Run after all rows are processed. Failures are logged as WARN in MongoDB
`processing_logs`. The file status is not changed.

| Rule | Check |
|------|-------|
| **V25** | `TRAILER.total_amount_cents` must equal sum of all valid DETAIL `amount_cents` |
| **V26** | `TRAILER.total_debit_cents` must equal sum of DETAIL `amount_cents` where `type = DEBIT` |
| **V27** | `TRAILER.total_credit_cents` must equal sum where `type = CREDIT` |
| **V28** | `TRAILER.total_transfer_cents` must equal sum where `type = TRANSFER` |
| **V29** | `TRAILER.total_flagged` must equal count of DETAIL where `flagged = Y` |

---

## Implementation pattern

```java
// Domain service — no Spring imports
public class RowValidationService {

    public List<ValidationViolation> validate(ParsedRow row, FileContext ctx) {
        List<ValidationViolation> violations = new ArrayList<>();

        // V12
        if (row.externalRef() == null || row.externalRef().isBlank()) {
            violations.add(new ValidationViolation("V12", "INVALID_EXTERNAL_REF",
                "external_ref must not be empty"));
        } else if (ctx.seenExternalRefs().contains(row.externalRef())) {
            violations.add(new ValidationViolation("V12", "INVALID_EXTERNAL_REF",
                "external_ref must be unique within the file: " + row.externalRef()));
        }

        // V17
        if (row.amountCents() < 0) {
            violations.add(new ValidationViolation("V17", "INVALID_AMOUNT",
                "amount_cents must be >= 0, got: " + row.amountCents()));
        }

        // ... remaining rules

        return violations;
    }
}
```

```java
// In the orchestrator: decide what to do with violations
if (!violations.isEmpty()) {
    // Any V11-V24 violation → skip row
    warningRepository.saveAll(toWarnings(row, violations));
    auditRepository.logRowError(fileId, row.lineNumber(), violations);
    result.incrementFailed();
} else {
    // Check for non-fatal warnings (MISSING_DESCRIPTION)
    checkNonFatalWarnings(row).forEach(w -> warningRepository.save(w));
    transactionRepository.enqueue(toTransaction(row, fileId));
    result.incrementSuccess();
}
```

---

## FileContext — state carried across rows

```java
public record FileContext(
    UUID        fileId,
    LocalDate   periodFrom,
    LocalDate   periodTo,
    Set<String> seenExternalRefs   // for V12 uniqueness check
) {}
```

Build `FileContext` from the parsed HEADER before processing any DETAIL row.
