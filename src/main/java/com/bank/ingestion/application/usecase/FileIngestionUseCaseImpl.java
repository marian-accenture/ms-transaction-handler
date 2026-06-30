package com.bank.ingestion.application.usecase;

import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.FileStatus;
import com.bank.ingestion.domain.model.IngestionSummary;
import com.bank.ingestion.domain.port.inbound.FileIngestionUseCase;
import com.bank.ingestion.domain.port.outbound.FileParserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class FileIngestionUseCaseImpl implements FileIngestionUseCase {

    private final FileParserResolver fileParserResolver;

    @Autowired
    public FileIngestionUseCaseImpl(FileParserResolver fileParserResolver) {
        this.fileParserResolver = fileParserResolver;
    }

    @Override
    public IngestionSummary ingest(UUID fileId, Path filePath, String fileName,
                                   FileFormat fileFormat, String uploadedBy) {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(uploadedBy, "uploadedBy must not be null");

        fileParserResolver.ensureSupported(filePath);

        // Base use-case flow: parser support is validated and processing is ready to continue.
        IngestionSummary summary = new IngestionSummary();
        summary.setFileId(fileId);
        summary.setStatus(FileStatus.PROCESSING);
        summary.setTotalRows(0);
        summary.setSuccessRows(0);
        summary.setFailedRows(0);
        return summary;
    }
}
