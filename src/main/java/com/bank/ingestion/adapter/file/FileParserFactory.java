package com.bank.ingestion.adapter.file;

import com.bank.ingestion.adapter.file.parser.FileParserStrategy;
import com.bank.ingestion.domain.exception.FileParsingException;
import com.bank.ingestion.domain.exception.UnsupportedFormatException;
import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.model.ParsedFile;
import com.bank.ingestion.domain.port.outbound.FileParserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileParserFactory implements FileParserPort {

    private final Map<FileFormat, FileParserStrategy> strategies;

    public FileParserFactory(List<FileParserStrategy> parsers) {
        this.strategies = parsers.stream()
                .collect(Collectors.toMap(FileParserStrategy::supportedFormat, Function.identity()));
    }

    @Override
    public ParsedFile parse(String filePath, String fileFormat, UUID fileId)
            throws FileParsingException, UnsupportedFormatException {
        FileFormat format;
        try {
            format = FileFormat.valueOf(fileFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedFormatException(fileFormat);
        }

        FileParserStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new UnsupportedFormatException(fileFormat);
        }

        log.debug("Parsing file {} with strategy {}", filePath, format);
        return strategy.parse(filePath, fileId);
    }
}
