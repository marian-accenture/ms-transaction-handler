# App 1 — Testing Requirements

## Coverage target

**80 % line coverage** is a hard CI gate. The build fails below this threshold.

```xml
<!-- pom.xml — JaCoCo -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.80</minimum>
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</plugin>
```

---

## Test pyramid

```
         [ E2E / Contract ]      ← optional, out of scope for App 1
        [  Integration     ]     ← Spring context, real DB (Testcontainers)
       [    Unit            ]    ← pure Java, no Spring, fast
```

---

## Unit tests

No Spring context. Test domain and application layer classes in isolation.

### What to cover

| Class | Test focus |
|-------|-----------|
| `RowValidationService` | All 14 row rules (V11–V24) — one test per rule, happy path + each violation |
| `FileValidationService` | All 10 file rules (V01–V10) |
| `ReconciliationService` | V25–V29 — mismatch and match |
| `AccountResolutionService` | Existing account found, new account created, CBU conflict |
| `TxtFixedWidthParser` | Correct tokenization, padding/trimming, amount_cents parsing |
| `IngestionOrchestrator` | Full flow with mocked ports — success, duplicate, all-rows-fail |

### Example: row validation

```java
class RowValidationServiceTest {

    private final RowValidationService sut = new RowValidationService();

    @Test
    void v19_benefactorCbuMustBe22Digits() {
        ParsedRow row = ParsedRow.builder()
            .externalRef("TNX-001")
            .benefactorCbu("12345")  // too short
            // ... other valid fields
            .build();

        List<ValidationViolation> violations = sut.validate(row, aValidContext());

        assertThat(violations)
            .extracting(ValidationViolation::code)
            .containsExactly("V19");
        assertThat(violations.get(0).warningCode())
            .isEqualTo("INVALID_BENEFACTOR_CBU");
    }

    @Test
    void v23_selfTransferIsRejected() {
        String sameCbu = "0000003100012345678901";
        ParsedRow row = validRow()
            .benefactorCbu(sameCbu)
            .beneficiaryCbu(sameCbu)
            .build();

        assertThat(sut.validate(row, aValidContext()))
            .extracting(ValidationViolation::code)
            .contains("V23");
    }
}
```

---

## Integration tests

Use **Testcontainers** for real PostgreSQL and MongoDB instances.  
Use **EmbeddedKafkaBroker** (`@EmbeddedKafka`) for Kafka.

```java
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1,
               topics = {"transaction.file.ready", "transaction.file.processed"})
class IngestionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Container
    static MongoDBContainer mongo =
        new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }
}
```

### Integration test scenarios

| Scenario | Verify |
|----------|--------|
| Happy path — 3 valid rows | `transactions` count=3, `ingested_files.status=COMPLETED`, `FileProcessedEvent` published |
| Duplicate file | Second send → `status=DUPLICATE`, no new transactions, event published |
| V08 checksum mismatch | `status=FAILED`, 0 transactions, error in MongoDB log |
| Mixed rows (2 valid, 1 invalid) | `success_rows=2`, `failed_rows=1`, 1 warning in DB |
| All rows invalid | `status=FAILED`, 0 transactions, failedRows=3 |
| DB down during processing | Kafka offset NOT committed, retry on reconnect |
| Amount conversion | `transaction.amount` in DB = raw cents; API response = cents/100 |

---

## Architecture tests (ArchUnit)

```java
@AnalyzeClasses(packages = "com.bank.ingestion")
class HexagonalArchitectureTest {

    @ArchTest
    ArchRule domainHasNoFrameworkDeps = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.apache.kafka..",
            "com.mongodb.."
        );

    @ArchTest
    ArchRule adaptersDoNotDependOnEachOther = noClasses()
        .that().resideInAPackage("..adapter.inbound..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter.outbound..");

    @ArchTest
    ArchRule applicationDoesNotDependOnAdapters = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter..");
}
```

---

## File parser tests

Test each parser strategy with real sample files:

```java
class TxtFixedWidthParserTest {

    private final TxtFixedWidthParser parser = new TxtFixedWidthParser();

    @Test
    void parsesDetailRowCorrectly() throws Exception {
        Path file = Paths.get("src/test/resources/fixtures/sample_3rows.txt");
        List<ParsedRow> rows = parser.parse(file, UUID.randomUUID());

        assertThat(rows).hasSize(3);
        ParsedRow first = rows.get(0);
        assertThat(first.externalRef()).isEqualTo("TNX-2024-000001");
        assertThat(first.amountCents()).isEqualTo(25000L);
        assertThat(first.currency()).isEqualTo("USD");
        assertThat(first.benefactorCbu()).isEqualTo("0000003100012345678901");
        assertThat(first.flagged()).isFalse();
    }

    @Test
    void rejectsFileWithCrlfLineEndings() {
        Path crlf = Paths.get("src/test/resources/fixtures/crlf_file.txt");
        assertThatThrownBy(() -> parser.parse(crlf, UUID.randomUUID()))
            .isInstanceOf(FileParsingException.class)
            .hasMessageContaining("CRLF");
    }
}
```

Place test fixture files in `src/test/resources/fixtures/`:

| File | Content |
|------|---------|
| `sample_3rows.txt` | Valid file with 3 DETAIL rows (from spec example) |
| `duplicate_checksum.txt` | Same checksum as `sample_3rows.txt` |
| `invalid_checksum.txt` | Valid structure but wrong HEADER.checksum |
| `all_rows_invalid.txt` | All DETAIL rows fail V11–V24 |
| `mixed_rows.txt` | 2 valid + 1 invalid row |
| `crlf_file.txt` | CRLF line endings |
| `missing_description.txt` | Row with empty description field |

---

## Test naming convention

```
methodUnderTest_scenario_expectedOutcome

Examples:
  validate_whenBenefactorCbuHas21Digits_returnsV19Violation
  ingest_whenDuplicateChecksum_publishesDuplicateEvent
  parse_whenCrlfLineEndings_throwsFileParsingException
```
