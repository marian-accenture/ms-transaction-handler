package com.bank.ingestion.adapter.file;

import com.bank.ingestion.adapter.file.parser.FileParserStrategy;
import com.bank.ingestion.adapter.file.parser.UnsupportedFormatException;
import com.bank.ingestion.domain.model.FileFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FileParserFactory {

    private final Map<FileFormat, FileParserStrategy> strategies;

    public FileParserFactory(List<FileParserStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FileParserStrategy::supportedFormat, Function.identity()));
    }

    public FileParserStrategy getParser(FileFormat format) {
        FileParserStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new UnsupportedFormatException(format);
        }
        return strategy;
    }
}
