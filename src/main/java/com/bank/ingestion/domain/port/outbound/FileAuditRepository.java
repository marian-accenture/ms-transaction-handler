package com.bank.ingestion.domain.port.outbound;

import java.util.UUID;

public interface FileAuditRepository {

    void logProcessingStart(UUID fileId, String fileName);

    void logProcessingComplete(UUID fileId, int successRows, int failedRows);

    void logProcessingError(UUID fileId, String errorMessage);
}
