package com.bank.ingestion.application.usecase;

import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.IngestionSummary;
import com.bank.ingestion.domain.port.inbound.FileIngestionUseCase;
import com.bank.ingestion.domain.port.outbound.FileParserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        fileParserResolver.ensureSupported(filePath);
        throw new UnsupportedOperationException("TODO: implement ingestion flow");
    }
}
