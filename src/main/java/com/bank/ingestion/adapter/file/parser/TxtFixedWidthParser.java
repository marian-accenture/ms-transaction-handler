package com.bank.ingestion.adapter.file.parser;

import com.bank.ingestion.application.dto.ParsedRow;
import com.bank.ingestion.domain.model.FileFormat;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class TxtFixedWidthParser implements FileParserStrategy {

    @Override
    public List<ParsedRow> parse(Path filePath, UUID fileId) throws FileParsingException {
        // TODO: use Spring Batch FlatFileItemReader with FixedLengthTokenizer (chunk=500)
        throw new UnsupportedOperationException("TODO: implement TXT fixed-width parser");
    }

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.TXT;
    }
}
