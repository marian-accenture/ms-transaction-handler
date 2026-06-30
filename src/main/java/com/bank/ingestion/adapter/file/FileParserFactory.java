package com.bank.ingestion.adapter.file;

import com.bank.ingestion.adapter.file.parser.FileParserStrategy;
import com.bank.ingestion.adapter.file.parser.UnsupportedFormatException;
import com.bank.ingestion.domain.model.FileFormat;
import com.bank.ingestion.domain.port.outbound.FileParserResolver;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Locale;

@Component
public class FileParserFactory implements FileParserResolver {

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

    public FileParserStrategy getParser(Path filePath) {
        return getParser(resolveFormat(filePath));
    }

    @Override
    public void ensureSupported(Path filePath) {
        getParser(filePath);
    }

    private FileFormat resolveFormat(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".txt")) {
            return FileFormat.TXT;
        }
        if (fileName.endsWith(".json")) {
            return FileFormat.JSON;
        }
        throw new UnsupportedFormatException(fileName);
    }
}
