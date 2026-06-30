package com.bank.ingestion.domain.port.outbound;

import java.nio.file.Path;

public interface FileParserResolver {

    void ensureSupported(Path filePath);
}