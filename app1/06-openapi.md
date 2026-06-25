# App 1 — Internal Admin REST API

## Scope

App 1 exposes a **cluster-internal** REST API for operational monitoring only.  
It is **not** published to the public API gateway.

Base path: `/internal/v1`  
Full spec: `app1-ingestion-openapi.yaml`

---

## Endpoints summary

### Health

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness/readiness — returns status of Postgres, Mongo, Kafka consumer + producer |

Kubernetes probe config:

```yaml
livenessProbe:
  httpGet:
    path: /internal/v1/health
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /internal/v1/health
    port: 8081
  failureThreshold: 3
```

---

### Files

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/files` | List ingested files. Filterable by `status`, `ingestionDateFrom/To`, `uploadedBy`, `fileFormat`. Paginated. |
| `GET` | `/files/{fileId}` | Full file detail: metadata + parsed HEADER/TRAILER + MongoDB processing log. |
| `POST` | `/files/{fileId}/retry` | Re-queue a `FAILED` file to Kafka `transaction.file.ready`. Returns `202 Accepted`. |
| `GET` | `/files/{fileId}/warnings` | Row-level validation warnings from `transaction_validation_warnings`. Filterable by `warningCode`. |

**Retry preconditions:**
- File must exist.
- File status must be `FAILED`. Returns `409` for `COMPLETED` or `DUPLICATE`.
- The retry re-publishes a `FileReadyEvent` with a new `eventId`. The original processing log is preserved and the new run is appended.

---

### Transactions (file-scoped)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/files/{fileId}/transactions` | Transactions persisted from a specific file. Filterable by `status`, `flagged`. |
| `GET` | `/files/{fileId}/transactions/{transactionId}` | Single transaction detail with validation warnings. |

> For cross-file queries, use **App 2 Query API** (`GET /api/v1/transactions`).

---

## Common query parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Zero-based page number (default: 0) |
| `size` | integer | Page size 1–100 (default: 20) |
| `sort` | string | Field and direction, e.g. `uploadedAt,desc` |
| `ingestionDateFrom` | date `yyyy-MM-dd` | Filter on `ingested_files.processed_at` ≥ |
| `ingestionDateTo` | date `yyyy-MM-dd` | Filter on `ingested_files.processed_at` ≤ |

---

## Error response shape

All errors return `application/json`:

```json
{
  "timestamp": "2024-03-15T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "ingestionDateFrom must not be after ingestionDateTo",
  "path": "/internal/v1/files",
  "details": [
    {
      "field": "status",
      "rejectedValue": "INVALID",
      "message": "must be one of: RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE"
    }
  ]
}
```

HTTP status codes used:

| Code | When |
|------|------|
| `200` | Success |
| `202` | Retry accepted (async) |
| `400` | Invalid query parameters |
| `404` | File or transaction not found |
| `409` | File not in retryable state |
| `500` | Unexpected server error |
| `503` | Service unhealthy (health endpoint only) |

---

## Controller implementation notes

- Controllers live in `adapter/inbound/rest/` — no domain logic here.
- Map domain exceptions to HTTP status in a `@RestControllerAdvice`.
- Use `@PageableDefault(size = 20, sort = "uploadedAt", direction = DESC)` for paginated endpoints.
- The retry endpoint is idempotent on `fileId` — if a retry is already `PROCESSING`, return `409`.

```java
@RestController
@RequestMapping("/internal/v1/files")
@RequiredArgsConstructor
class FileAdminController {

    private final FileIngestionUseCase fileUseCase;
    private final FileAdminMapper mapper;

    @GetMapping
    Page<FileMetadataResponse> listFiles(
        @RequestParam(required = false) FileStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate ingestionDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate ingestionDateTo,
        @RequestParam(required = false) String uploadedBy,
        Pageable pageable
    ) {
        return fileUseCase.listFiles(status, ingestionDateFrom, ingestionDateTo, uploadedBy, pageable)
            .map(mapper::toResponse);
    }

    @PostMapping("/{fileId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RetryResponse retryFile(
        @PathVariable UUID fileId,
        @RequestBody(required = false) RetryRequest request
    ) {
        return mapper.toRetryResponse(fileUseCase.retryFile(fileId, request));
    }
}
```
