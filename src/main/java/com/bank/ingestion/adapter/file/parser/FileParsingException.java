package com.bank.ingestion.adapter.file.parser;

public class FileParsingException extends Exception {

    public FileParsingException(String message) {
        super(message);
    }

    public FileParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
