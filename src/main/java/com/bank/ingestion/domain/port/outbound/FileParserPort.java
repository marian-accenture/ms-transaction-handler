package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.exception.FileParsingException;
import com.bank.ingestion.domain.exception.UnsupportedFormatException;
import com.bank.ingestion.domain.model.ParsedFile;

import java.util.UUID;

public interface FileParserPort {

    ParsedFile parse(String filePath, String fileFormat, UUID fileId)
            throws FileParsingException, UnsupportedFormatException;
}
