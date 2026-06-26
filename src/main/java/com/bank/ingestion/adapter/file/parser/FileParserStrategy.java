package com.bank.ingestion.adapter.file.parser;

import com.bank.ingestion.application.dto.ParsedRow;
import com.bank.ingestion.domain.model.FileFormat;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface FileParserStrategy {

    List<ParsedRow> parse(Path filePath, UUID fileId) throws FileParsingException;

    FileFormat supportedFormat();
}
