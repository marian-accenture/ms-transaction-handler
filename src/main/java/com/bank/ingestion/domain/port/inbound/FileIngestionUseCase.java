package com.bank.ingestion.domain.port.inbound;

import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.IngestionSummary;

import java.nio.file.Path;
import java.util.UUID;

public interface FileIngestionUseCase {

    IngestionSummary ingest(UUID fileId, Path filePath, String fileName,
                            FileFormat fileFormat, String uploadedBy);
}
