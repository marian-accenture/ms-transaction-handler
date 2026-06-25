package com.bank.ingestion.adapter.file.parser;

import com.bank.ingestion.domain.exception.FileParsingException;
import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.ParsedFile;

import java.util.UUID;

public interface FileParserStrategy {

    ParsedFile parse(String filePath, UUID fileId) throws FileParsingException;

    FileFormat supportedFormat();
}
