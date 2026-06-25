package com.bank.ingestion.application.usecase;

import com.bank.ingestion.domain.exception.DatabaseException;
import com.bank.ingestion.domain.exception.FileNotFoundException;
import com.bank.ingestion.domain.exception.UnsupportedFormatException;
import com.bank.ingestion.domain.model.FileContext;
import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.FileProcessedCommand;
import com.bank.ingestion.domain.model.FileReadyCommand;
import com.bank.ingestion.domain.model.FileStatus;
import com.bank.ingestion.domain.model.IngestedFile;
import com.bank.ingestion.domain.model.PagedResult;
import com.bank.ingestion.domain.model.ParsedFile;
import com.bank.ingestion.domain.model.ParsedRow;
import com.bank.ingestion.domain.model.Transaction;
import com.bank.ingestion.domain.model.TransactionStatus;
import com.bank.ingestion.domain.model.TransactionType;
import com.bank.ingestion.domain.model.ValidationViolation;
import com.bank.ingestion.domain.model.ValidationWarning;
import com.bank.ingestion.domain.port.inbound.FileIngestionUseCase;
import com.bank.ingestion.domain.port.outbound.AccountRepository;
import com.bank.ingestion.domain.port.outbound.FileAuditRepository;
import com.bank.ingestion.domain.port.outbound.FileParserPort;
import com.bank.ingestion.domain.port.outbound.FileProcessedEventPublisher;
import com.bank.ingestion.domain.port.outbound.IdempotencyStore;
import com.bank.ingestion.domain.port.outbound.IngestedFileRepository;
import com.bank.ingestion.domain.port.outbound.TransactionRepository;
import com.bank.ingestion.domain.port.outbound.ValidationWarningRepository;
import com.bank.ingestion.domain.service.AccountResolutionService;
import com.bank.ingestion.domain.service.FileValidationService;
import com.bank.ingestion.domain.service.ReconciliationService;
import com.bank.ingestion.domain.service.RowValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileIngestionUseCaseImpl implements FileIngestionUseCase {

    private final IdempotencyStore idempotencyStore;
    private final IngestedFileRepository fileRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ValidationWarningRepository warningRepository;
    private final FileAuditRepository auditRepository;
    private final FileProcessedEventPublisher eventPublisher;
    private final FileParserPort fileParserPort;
    private final FileValidationService fileValidationService;
    private final RowValidationService rowValidationService;
    private final ReconciliationService reconciliationService;
    private final AccountResolutionService accountResolutionService;

    @Value("${app.ingestion.batch.chunk-size:500}")
    private int chunkSize;

    public FileIngestionUseCaseImpl(
            IdempotencyStore idempotencyStore,
            IngestedFileRepository fileRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            ValidationWarningRepository warningRepository,
            FileAuditRepository auditRepository,
            FileProcessedEventPublisher eventPublisher,
            FileParserPort fileParserPort,
            FileValidationService fileValidationService,
            RowValidationService rowValidationService,
            ReconciliationService reconciliationService,
            AccountResolutionService accountResolutionService
    ) {
        this.idempotencyStore = idempotencyStore;
        this.fileRepository = fileRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.warningRepository = warningRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
        this.fileParserPort = fileParserPort;
        this.fileValidationService = fileValidationService;
        this.rowValidationService = rowValidationService;
        this.reconciliationService = reconciliationService;
        this.accountResolutionService = accountResolutionService;
    }

    @Override
    public void ingest(FileReadyCommand command) {
        // Step 1: Idempotency check
        if (idempotencyStore.isAlreadyProcessed(command.eventId())) {
            log.info("Event {} already processed, skipping", command.eventId());
            return;
        }

        FileFormat format;
        try {
            format = FileFormat.valueOf(command.fileFormat().toUpperCase());
        } catch (IllegalArgumentException e) {
            handleUnsupportedFormat(command);
            idempotencyStore.markProcessed(command.eventId());
            return;
        }

        // Step 2: Duplicate file check
        if (fileRepository.findByChecksum(command.checksum()).isPresent()) {
            handleDuplicate(command);
            idempotencyStore.markProcessed(command.eventId());
            return;
        }

        // Record file as RECEIVED
        IngestedFile ingestedFile = IngestedFile.create(
                null, // id comes from HEADER for TXT; set after parse
                command.fileName(),
                format,
                command.checksum(),
                command.fileSizeBytes(),
                command.uploadedBy(),
                command.uploadedAt()
        );

        List<Map<String, Object>> logEntries = new ArrayList<>();

        try {
            // Step 3: Parse file
            ParsedFile parsedFile = fileParserPort.parse(command.filePath(), command.fileFormat(), null);
            UUID fileId = parsedFile.header() != null ? parsedFile.header().fileId() : UUID.randomUUID();

            ingestedFile = IngestedFile.create(
                    fileId,
                    command.fileName(),
                    format,
                    command.checksum(),
                    command.fileSizeBytes(),
                    command.uploadedBy(),
                    command.uploadedAt()
            );

            logEntries.add(logEntry("INFO", "PARSE", "File parsed: " + parsedFile.rows().size() + " DETAIL rows", null, null));

            // Insert ingested_files with status=RECEIVED
            fileRepository.save(ingestedFile);

            ingestedFile.markProcessing();
            fileRepository.update(ingestedFile);

            // Step 4: File-level validation (V01-V10)
            List<String> middleRecTypes = parsedFile.rows().stream().map(ParsedRow::recType).toList();
            boolean checksumExists = fileRepository.findByChecksum(command.checksum())
                    .filter(f -> !f.getId().equals(fileId))
                    .isPresent();

            List<ValidationViolation> fileViolations = fileValidationService.validate(
                    parsedFile.header() != null ? "H" : "?",
                    parsedFile.trailer() != null ? "T" : "?",
                    middleRecTypes,
                    parsedFile.rows().size(),
                    parsedFile.header(),
                    parsedFile.trailer(),
                    parsedFile.computedDetailChecksum(),
                    checksumExists
            );

            if (!fileViolations.isEmpty()) {
                String errorMsg = fileViolations.get(0).message();
                logEntries.add(logEntry("ERROR", "VALIDATE", "File-level validation failed: " + errorMsg, null, null));
                ingestedFile.markFailed(Instant.now());
                fileRepository.update(ingestedFile);
                flushAuditAndPublish(ingestedFile, parsedFile, logEntries, errorMsg);
                idempotencyStore.markProcessed(command.eventId());
                return;
            }

            logEntries.add(logEntry("INFO", "VALIDATE", "File-level validation passed (V01-V10)", null, null));

            // Step 5: Row-level processing
            Instant ingestedAt = Instant.now();
            FileContext ctx = new FileContext(fileId, parsedFile.header().periodFrom(), parsedFile.header().periodTo(), new HashSet<>());

            int successRows = 0;
            int failedRows = 0;
            long totalCents = 0, debitCents = 0, creditCents = 0, transferCents = 0;
            int flaggedCount = 0;

            List<Transaction> chunkBuffer = new ArrayList<>();
            List<ValidationWarning> warningBuffer = new ArrayList<>();

            for (ParsedRow row : parsedFile.rows()) {
                List<ValidationViolation> violations = rowValidationService.validate(row, ctx);

                if (!violations.isEmpty()) {
                    failedRows++;
                    for (ValidationViolation v : violations) {
                        warningBuffer.add(ValidationWarning.forSkippedRow(fileId, row.lineNumber(), v.warningCode(), v.message()));
                        logEntries.add(logEntry("ERROR", "VALIDATE", v.message(), row.lineNumber(), Map.of(
                                "field", v.warningCode(), "validationRule", v.ruleCode(), "externalRef", row.externalRef() != null ? row.externalRef() : "")));
                    }
                } else {
                    // Enrich: resolve/create accounts
                    UUID benefactorId = accountResolutionService.resolveOrCreate(
                            row.benefactorCbu(), row.benefactorCuit(), row.benefactorName());
                    UUID beneficiaryId = accountResolutionService.resolveOrCreate(
                            row.beneficiaryCbu(), row.beneficiaryCuit(), row.beneficiaryName());

                    Transaction tx = Transaction.of(
                            row.externalRef().trim(),
                            row.transactionAt(),
                            ingestedAt,
                            TransactionType.valueOf(row.type().trim()),
                            TransactionStatus.valueOf(row.status().trim()),
                            row.amountCents(),
                            row.currency().trim(),
                            benefactorId,
                            beneficiaryId,
                            row.description() != null ? row.description().trim() : null,
                            fileId,
                            command.uploadedBy(),
                            row.isFlaggedBool()
                    );
                    chunkBuffer.add(tx);

                    // Non-fatal warnings
                    List<ValidationViolation> nonFatal = rowValidationService.checkNonFatalWarnings(row);
                    for (ValidationViolation w : nonFatal) {
                        warningBuffer.add(ValidationWarning.forSkippedRow(fileId, row.lineNumber(), w.warningCode(), w.message()));
                    }

                    successRows++;
                    totalCents += row.amountCents();
                    if ("DEBIT".equals(row.type().trim())) debitCents += row.amountCents();
                    if ("CREDIT".equals(row.type().trim())) creditCents += row.amountCents();
                    if ("TRANSFER".equals(row.type().trim())) transferCents += row.amountCents();
                    if (row.isFlaggedBool()) flaggedCount++;

                    // Commit chunk when full
                    if (chunkBuffer.size() >= chunkSize) {
                        transactionRepository.saveAll(chunkBuffer);
                        warningRepository.saveAll(warningBuffer);
                        chunkBuffer.clear();
                        warningBuffer.clear();
                    }
                }
            }

            // Flush remaining chunk
            if (!chunkBuffer.isEmpty()) {
                transactionRepository.saveAll(chunkBuffer);
            }
            if (!warningBuffer.isEmpty()) {
                warningRepository.saveAll(warningBuffer);
            }

            // Step 6: Reconciliation (V25-V29) - warnings only
            if (parsedFile.trailer() != null) {
                List<ValidationViolation> recon = reconciliationService.reconcile(
                        parsedFile.trailer(), totalCents, debitCents, creditCents, transferCents, flaggedCount);
                for (ValidationViolation v : recon) {
                    logEntries.add(logEntry("WARN", "RECONCILE", v.message(), null, null));
                }
            }

            // Step 7: Write file metadata
            int total = parsedFile.rows().size();
            ingestedFile.markCompleted(total, successRows, failedRows, ingestedAt);
            fileRepository.update(ingestedFile);

            // Step 8: MongoDB audit
            flushAuditAndPublish(ingestedFile, parsedFile, logEntries, null);

        } catch (UnsupportedFormatException e) {
            handleUnsupportedFormat(command);
        } catch (Exception e) {
            if (isDbError(e)) {
                throw new DatabaseException("Database error during ingestion", e);
            }
            log.error("Unexpected error ingesting file {}", command.fileName(), e);
            throw e;
        }

        idempotencyStore.markProcessed(command.eventId());
    }

    private void handleDuplicate(FileReadyCommand command) {
        log.info("Duplicate file detected for checksum {}", command.checksum());
        FileProcessedCommand event = new FileProcessedCommand(
                UUID.randomUUID(), null, command.fileName(), "DUPLICATE",
                null, null, null, null, Instant.now(), null);
        publishSafely(event);
    }

    private void handleUnsupportedFormat(FileReadyCommand command) {
        log.warn("Unsupported file format: {}", command.fileFormat());
        FileProcessedCommand event = new FileProcessedCommand(
                UUID.randomUUID(), null, command.fileName(), "FAILED",
                null, null, null, null, Instant.now(), "Unsupported format: " + command.fileFormat());
        publishSafely(event);
    }

    private void flushAuditAndPublish(IngestedFile file, ParsedFile parsedFile,
                                       List<Map<String, Object>> logEntries, String errorMessage) {
        Map<String, Object> headerData = parsedFile != null && parsedFile.header() != null
                ? headerToMap(parsedFile) : Map.of();
        Map<String, Object> trailerData = parsedFile != null && parsedFile.trailer() != null
                ? trailerToMap(parsedFile) : Map.of();

        try {
            auditRepository.saveFileUpload(file, headerData, trailerData);
            auditRepository.appendProcessingLogs(file.getId(), logEntries);
        } catch (Exception e) {
            log.error("MongoDB audit write failed for file {}", file.getId(), e);
            throw new DatabaseException("MongoDB write failed", e);
        }

        FileProcessedCommand event = new FileProcessedCommand(
                UUID.randomUUID(),
                file.getId(),
                file.getFileName(),
                file.getStatus().name(),
                file.getTotalRows(),
                file.getSuccessRows(),
                file.getFailedRows(),
                null,
                file.getProcessedAt() != null ? file.getProcessedAt() : Instant.now(),
                errorMessage
        );
        publishSafely(event);
    }

    private void publishSafely(FileProcessedCommand event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Kafka publish failed for file {}: {}", event.fileId(), e.getMessage());
        }
    }

    private boolean isDbError(Exception e) {
        return e.getClass().getName().contains("JdbcSQLException")
                || e.getClass().getName().contains("DataAccessException")
                || e.getClass().getName().contains("SQLException");
    }

    private Map<String, Object> headerToMap(ParsedFile parsedFile) {
        var h = parsedFile.header();
        Map<String, Object> m = new HashMap<>();
        m.put("fileId", h.fileId());
        m.put("fileVersion", h.fileVersion());
        m.put("originSystem", h.originSystem());
        m.put("branchCode", h.branchCode());
        m.put("generatedBy", h.generatedBy());
        m.put("generatedAt", h.generatedAt());
        m.put("periodFrom", h.periodFrom());
        m.put("periodTo", h.periodTo());
        m.put("currency", h.currency());
        m.put("totalRecords", h.totalRecords());
        return m;
    }

    private Map<String, Object> trailerToMap(ParsedFile parsedFile) {
        var t = parsedFile.trailer();
        Map<String, Object> m = new HashMap<>();
        m.put("totalDetailLines", t.totalDetailLines());
        m.put("totalAmountCents", t.totalAmountCents());
        m.put("totalDebitCents", t.totalDebitCents());
        m.put("totalCreditCents", t.totalCreditCents());
        m.put("totalTransferCents", t.totalTransferCents());
        m.put("totalFlagged", t.totalFlagged());
        m.put("periodFrom", t.periodFrom());
        m.put("periodTo", t.periodTo());
        m.put("fileId", t.fileId());
        return m;
    }

    private Map<String, Object> logEntry(String level, String phase, String message, Integer lineNumber, Map<String, Object> data) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("level", level);
        entry.put("phase", phase);
        entry.put("message", message);
        entry.put("lineNumber", lineNumber);
        entry.put("data", data != null ? data : Map.of());
        return entry;
    }

    @Override
    public PagedResult<IngestedFile> listFiles(FileStatus status, LocalDate from, LocalDate to, String uploadedBy, int page, int size) {
        return fileRepository.findAll(status, from, to, uploadedBy, page, size);
    }

    @Override
    public IngestedFile getFile(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    @Override
    public void retryFile(UUID fileId) {
        IngestedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
        if (file.getStatus() != FileStatus.FAILED) {
            throw new com.bank.ingestion.domain.exception.DomainException(
                    "File " + fileId + " is not in FAILED status, cannot retry. Current status: " + file.getStatus());
        }
        // Re-publish a FileReadyEvent via a dedicated port would be cleaner;
        // here we reset status so the admin controller can re-trigger.
        file.setStatus(FileStatus.RECEIVED);
        fileRepository.update(file);
    }

    @Override
    public List<ValidationWarning> getWarnings(UUID fileId, String warningCode) {
        return warningRepository.findByFileId(fileId, warningCode);
    }

    @Override
    public PagedResult<Transaction> getTransactions(UUID fileId, String status, Boolean flagged, int page, int size) {
        List<Transaction> all = transactionRepository.findByFileId(fileId);
        List<Transaction> filtered = all.stream()
                .filter(t -> status == null || t.getStatus().name().equals(status))
                .filter(t -> flagged == null || t.isFlagged() == flagged)
                .toList();
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new PagedResult<>(filtered.subList(from, to), total, (int) Math.ceil((double) total / size), page, size);
    }

    @Override
    public Transaction getTransaction(UUID fileId, UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .filter(t -> t.getFileId().equals(fileId))
                .orElseThrow(() -> new com.bank.ingestion.domain.exception.DomainException(
                        "Transaction " + transactionId + " not found in file " + fileId));
    }
}
