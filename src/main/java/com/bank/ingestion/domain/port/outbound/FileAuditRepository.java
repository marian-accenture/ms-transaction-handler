package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.model.IngestedFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FileAuditRepository {

    void saveFileUpload(IngestedFile file, Map<String, Object> headerData, Map<String, Object> trailerData);

    void appendProcessingLogs(UUID fileId, List<Map<String, Object>> logEntries);
}
