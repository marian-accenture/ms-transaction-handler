# App 1 — Kafka Contracts

## Topics

| Topic | Direction | Partition key | Group ID |
|-------|-----------|---------------|----------|
| `transaction.file.ready` | **Inbound** | — | `ingestion-svc` |
| `transaction.file.processed` | **Outbound** | `fileId` (UUID string) | — |

---

## Inbound: `FileReadyEvent`

Published by the upstream file-upload service. App 1 consumes this event
with `ack-mode: MANUAL_IMMEDIATE` — the offset is committed **only after**
the file is fully processed and the outbound event is published.

```java
// com.bank.commons.event.FileReadyEvent
public record FileReadyEvent(
    UUID    eventId,        // unique event ID — used for idempotency
    String  filePath,       // path/URI where the file can be read
    String  fileName,       // original filename
    String  fileFormat,     // "TXT" | "CSV" | "JSON" | "XML"
    String  checksum,       // SHA-256 hex — used for duplicate detection (V10)
    long    fileSizeBytes,
    String  branchCode,     // originating branch (matches HEADER.branch_code in TXT)
    String  uploadedBy,     // operator or service ID → transactions.created_by
    Instant uploadedAt
) {}
```

### Consumer configuration

```java
@KafkaListener(
    topics      = "${app.ingestion.kafka.file-ready-topic}",
    groupId     = "${app.ingestion.kafka.consumer-group}",
    containerFactory = "manualAckListenerContainerFactory"
)
public void onFileReady(
    @Payload FileReadyEvent event,
    Acknowledgment ack
) { ... }
```

```yaml
spring:
  kafka:
    consumer:
      group-id: ingestion-svc
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    listener:
      ack-mode: MANUAL_IMMEDIATE
```

### Idempotency

Before processing, check `eventId` against a short-lived idempotency store
(Redis or an in-memory cache with TTL = 24 h). If already seen: ack and return.

---

## Outbound: `FileProcessedEvent`

Published after **every** ingestion attempt, whether success or failure.  
Message key = `fileId` (UUID string) — guarantees ordering per file.

```java
// com.bank.commons.event.FileProcessedEvent
public record FileProcessedEvent(
    UUID        eventId,        // new UUID for this event
    UUID        fileId,         // matches ingested_files.id
    String      fileName,
    String      status,         // "COMPLETED" | "FAILED" | "DUPLICATE"
    Integer     totalRows,      // null for DUPLICATE / unsupported format
    Integer     successRows,
    Integer     failedRows,
    Integer     flaggedCount,
    Instant     processedAt,
    String      errorMessage    // null unless status = FAILED
) {}
```

### Producer configuration

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
```

---

## Ack / no-ack decision matrix

| Situation | Ack? | Published event |
|-----------|------|-----------------|
| Processing complete (any outcome) | ✅ Yes | `FileProcessedEvent` |
| Duplicate file | ✅ Yes | `FileProcessedEvent` (status=DUPLICATE) |
| Unsupported format | ✅ Yes | `FileProcessedEvent` (status=FAILED) |
| PostgreSQL connection error | ❌ **No** | None — Kafka redelivers |
| MongoDB write error | ❌ **No** | None — Kafka redelivers |
| Kafka producer failure | ✅ Yes | Log error only — file already persisted |

> **Critical:** never ack if the DB is in an unknown state. The offset must only
> advance when the processing result is durably stored.

---

## Downstream consumers of `transaction.file.processed`

| Consumer | Subscribes for |
|----------|----------------|
| App 2 — Query API | Cache invalidation / index refresh |
| App 3 — Report Service | Trigger report generation |

App 1 has no knowledge of these consumers. Publish the event regardless.
