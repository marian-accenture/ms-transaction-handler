package com.bank.ingestion.domain.exception;

public class UnsupportedFormatException extends DomainException {
    public UnsupportedFormatException(String format) {
        super("Unsupported file format: " + format);
    }
}
