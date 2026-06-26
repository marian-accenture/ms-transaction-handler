# App 1 — Architecture

## Pattern: Hexagonal (Ports & Adapters)

The domain package contains **zero Spring, JDBC, Kafka, or MongoDB imports**.  
All framework coupling is isolated to the `adapter` layer.

```
com.bank.ingestion/
│
├── domain/                          ← pure Java, no framework imports
│   ├── model/                       ← value objects, entities, enums
│   │   ├── Transaction.java
│   │   ├── Account.java
│   │   ├── IngestedFile.java
│   │   ├── ValidationWarning.java
│   │   ├── TransactionType.java     ← DEBIT, CREDIT, TRANSFER, PAYMENT, REFUND, FEE
│   │   ├── TransactionStatus.java   ← PENDING, COMPLETED, FAILED, REVERSED
│   │   └── FileStatus.java          ← RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE
│   │
│   ├── port/
│   │   ├── inbound/                 ← use-case interfaces (called by adapters)
│   │   │   └── FileIngestionUseCase.java
│   │   └── outbound/                ← repository / event interfaces (implemented by adapters)
│   │       ├── TransactionRepository.java
│   │       ├── AccountRepository.java
│   │       ├── IngestedFileRepository.java
│   │       ├── ValidationWarningRepository.java
│   │       ├── FileAuditRepository.java      ← MongoDB
│   │       └── FileProcessedEventPublisher.java
│   │
│   └── service/                     ← domain services, orchestration
│       ├── IngestionOrchestrator.java
│       ├── FileValidationService.java
│       ├── RowValidationService.java
│       └── AccountResolutionService.java
│
├── application/                     ← use-case implementations, DTOs
│   ├── usecase/
│   │   └── FileIngestionUseCaseImpl.java
│   └── dto/
│       ├── ParsedRow.java           ← intermediate: raw parsed fields, pre-validation
│       ├── FileIngestionResult.java
│       └── RowProcessingResult.java
│
└── adapter/                         ← framework code lives here only
    ├── inbound/
    │   ├── kafka/
    │   │   ├── FileReadyKafkaListener.java      ← @KafkaListener entry point
    │   │   ├── FileReadyEventMapper.java
    │   │   └── KafkaConsumerConfig.java
    │   └── rest/                                ← internal admin API
    │       ├── FileAdminController.java
    │       ├── TransactionAdminController.java
    │       ├── HealthController.java
    │       └── dto/                             ← request/response DTOs (REST only)
    │
    ├── outbound/
    │   ├── postgres/
    │   │   ├── TransactionJpaRepository.java
    │   │   ├── AccountJpaRepository.java
    │   │   ├── IngestedFileJpaRepository.java
    │   │   ├── ValidationWarningJpaRepository.java
    │   │   └── entity/                          ← @Entity classes
    │   ├── mongodb/
    │   │   ├── FileAuditMongoRepository.java
    │   │   └── document/                        ← @Document classes
    │   └── kafka/
    │       ├── FileProcessedEventKafkaPublisher.java
    │       └── KafkaProducerConfig.java
    │
    └── file/                                    ← file parsing (Strategy pattern)
        ├── parser/
        │   ├── FileParserStrategy.java          ← interface
        │   ├── TxtFixedWidthParser.java         ← Spring Batch FlatFileItemReader
        │   ├── CsvParser.java
        │   ├── JsonParser.java
        │   └── XmlParser.java
        └── FileParserFactory.java               ← selects strategy by FileFormat
```

---

## Dependency rule (enforced)

```
adapters  →  application  →  domain
                              ↑
                         no outward deps
```

Use **ArchUnit** in tests to enforce this:

```java
@AnalyzeClasses(packages = "com.bank.ingestion")
class HexagonalArchitectureTest {

    @ArchTest
    ArchRule domainHasNoFrameworkImports = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.apache.kafka.."
        );
}
```

---

## Spring Batch integration

TXT parsing uses **Spring Batch** `FlatFileItemReader` with `FixedLengthTokenizer`.  
Three separate readers, one per record type, routed via `ClassifierCompositeItemWriter`.

```
FlatFileItemReader (Header)   — rec_type = H
FlatFileItemReader (Detail)   — rec_type = D  ← main reader, chunk=500
FlatFileItemReader (Trailer)  — rec_type = T
```

Key Batch config:

| Parameter | Value |
|-----------|-------|
| Encoding | UTF-8 |
| Line terminator | LF only — reject CRLF |
| Record length | 300 characters |
| Skip lines | 0 (header is a data record) |
| Chunk size | 500 rows per commit |
| Retry on DB error | 3 attempts, 2 s back-off |

Detail record `FixedLengthTokenizer` column ranges:

| Field | Start | End |
|-------|-------|-----|
| `rec_type` | 1 | 1 |
| `external_ref` | 2 | 31 |
| `transaction_at` | 32 | 45 |
| `type` | 46 | 53 |
| `status` | 54 | 62 |
| `amount_cents` | 63 | 77 |
| `currency` | 78 | 80 |
| `benefactor_cbu` | 81 | 102 |
| `benefactor_cuit` | 103 | 113 |
| `benefactor_name` | 114 | 153 |
| `beneficiary_cbu` | 154 | 175 |
| `beneficiary_cuit` | 176 | 186 |
| `beneficiary_name` | 187 | 226 |
| `description` | 227 | 276 |
| `flagged` | 277 | 277 |
| `reserved` | 278 | 300 |

---

## File parser strategy

```java
public interface FileParserStrategy {
    List<ParsedRow> parse(Path filePath, UUID fileId) throws FileParsingException;
    FileFormat supportedFormat();
}
```

`FileParserFactory` selects the correct strategy based on `FileReadyEvent.fileFormat`.  
Unknown formats throw `UnsupportedFormatException` → file status `FAILED`.

---

## Configuration properties

All tunable values must be externalized under `app.ingestion.*`:

```yaml
app:
  ingestion:
    max-file-size-bytes: 524288000   # 500 MB
    max-detail-lines: 1000000
    batch:
      chunk-size: 500
      retry-attempts: 3
      retry-backoff-ms: 2000
    kafka:
      file-ready-topic: transaction.file.ready
      file-processed-topic: transaction.file.processed
      consumer-group: ingestion-svc
```
