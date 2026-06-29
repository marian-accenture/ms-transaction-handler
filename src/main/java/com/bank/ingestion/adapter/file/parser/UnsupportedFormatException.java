package com.bank.ingestion.adapter.file.parser;

import com.bank.ingestion.domain.model.FileFormat;

public class UnsupportedFormatException extends RuntimeException {

    public UnsupportedFormatException(FileFormat format) {
        super("Unsupported file format: " + format);
    }

    public UnsupportedFormatException(String fileName) {
        super("Unsupported file format for file: " + fileName);
    }
}
