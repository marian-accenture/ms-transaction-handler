# MongoDB Audit Test Evidence

![MongoDB audit test evidence visual](12-mongodb-audit-test-evidence.svg)

Date: 2026-06-30

## Scope

Evidence for audit/log functionality persisted in MongoDB collections:

- `file_uploads`
- `processing_logs`

## Test Suite Executed

- `src/test/java/com/bank/ingestion/adapter/outbound/mongodb/FileAuditMongoAdapterTest.java`
- `src/test/java/com/bank/ingestion/adapter/outbound/mongodb/MongoAuditIntegrationTest.java`

## Execution Result

- Passed: 5
- Failed: 0

## What Was Verified

### Unit tests — adapter behavior

`FileAuditMongoAdapterTest` verifies that:

1. `logProcessingStart(...)` saves data in both repositories (`file_uploads` and `processing_logs`).
2. `logProcessingComplete(...)` updates audit status to `COMPLETED` and persists counters.
3. `logProcessingError(...)` persists failure status and error log.

### Integration test — real MongoDB with Testcontainers

`MongoAuditIntegrationTest` verifies that:

1. Processing start + complete produces:
   - `file_uploads.status = COMPLETED`
   - counters persisted (`successRows`, `failedRows`)
   - `INFO` entries in `processing_logs`
2. Processing error produces:
   - `file_uploads.status = FAILED`
   - `errorMessage` persisted
   - `ERROR` entry appended in `processing_logs`

### Integration test — embedded MongoDB (no Docker required)

`MongoAuditEmbeddedIntegrationTest` verifies the same MongoDB persistence flow against embedded Mongo running in the JVM.

## Notes

- The integration test is configured with `@Testcontainers(disabledWithoutDocker = true)`.
- If Docker is not available in the execution environment, this test is skipped safely instead of failing the pipeline.
- For non-Docker environments, `MongoAuditEmbeddedIntegrationTest` provides executable evidence and exports JSON artifacts into `target/evidence`.
