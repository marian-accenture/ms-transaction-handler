package com.bank.ingestion.adapter.outbound.mongodb;

import com.bank.ingestion.adapter.outbound.mongodb.document.FileAuditDocument;
import com.bank.ingestion.adapter.outbound.mongodb.document.ProcessingLogDocument;
import com.bank.ingestion.adapter.outbound.mongodb.document.ProcessingLogEntry;
import com.bank.ingestion.domain.port.outbound.FileAuditRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class FileAuditMongoAdapter implements FileAuditRepository {

    private final FileAuditMongoRepository fileUploadRepository;
    private final ProcessingLogMongoRepository processingLogRepository;

    public FileAuditMongoAdapter(FileAuditMongoRepository fileUploadRepository,
                                ProcessingLogMongoRepository processingLogRepository) {
        this.fileUploadRepository = fileUploadRepository;
        this.processingLogRepository = processingLogRepository;
    }

    @Override
    public void logProcessingStart(UUID fileId, String fileName) {
        FileAuditDocument fileUpload = loadOrCreateFileUpload(fileId);
        fileUpload.setFileName(fileName);
        fileUpload.setStatus("PROCESSING");
        fileUpload.setStartedAt(Instant.now());
        fileUploadRepository.save(fileUpload);

        appendLog(fileId, "INFO", "INGEST", "Processing started for file " + fileName);
    }

    @Override
    public void logProcessingComplete(UUID fileId, int successRows, int failedRows) {
        FileAuditDocument fileUpload = loadOrCreateFileUpload(fileId);
        fileUpload.setStatus("COMPLETED");
        fileUpload.setSuccessRows(successRows);
        fileUpload.setFailedRows(failedRows);
        fileUpload.setCompletedAt(Instant.now());
        fileUploadRepository.save(fileUpload);

        appendLog(fileId, "INFO", "INGEST", "Processing completed. successRows=" + successRows + ", failedRows=" + failedRows);
    }

    @Override
    public void logProcessingError(UUID fileId, String errorMessage) {
        FileAuditDocument fileUpload = loadOrCreateFileUpload(fileId);
        fileUpload.setStatus("FAILED");
        fileUpload.setErrorMessage(errorMessage);
        fileUpload.setCompletedAt(Instant.now());
        fileUploadRepository.save(fileUpload);

        appendLog(fileId, "ERROR", "INGEST", errorMessage);
    }

    private FileAuditDocument loadOrCreateFileUpload(UUID fileId) {
        return fileUploadRepository.findById(fileId.toString())
                .orElseGet(() -> {
                    FileAuditDocument created = new FileAuditDocument();
                    created.setId(fileId.toString());
                    return created;
                });
    }

    private void appendLog(UUID fileId, String level, String phase, String message) {
        ProcessingLogDocument processingLog = loadOrCreateProcessingLog(fileId);
        ProcessingLogEntry entry = new ProcessingLogEntry();
        entry.setTimestamp(Instant.now());
        entry.setLevel(level);
        entry.setPhase(phase);
        entry.setMessage(message);
        processingLog.getEntries().add(entry);
        processingLogRepository.save(processingLog);
    }

    private ProcessingLogDocument loadOrCreateProcessingLog(UUID fileId) {
        return processingLogRepository.findById(fileId.toString())
                .orElseGet(() -> {
                    ProcessingLogDocument created = new ProcessingLogDocument();
                    created.setId(fileId.toString());
                    return created;
                });
    }
}