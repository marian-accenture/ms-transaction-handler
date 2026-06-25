package com.bank.ingestion.adapter.inbound.rest;

import com.bank.ingestion.adapter.inbound.rest.dto.ErrorResponse;
import com.bank.ingestion.domain.exception.DomainException;
import com.bank.ingestion.domain.exception.FileNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(FileNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDomainException(DomainException ex, HttpServletRequest req) {
        log.warn("Domain exception: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI());
    }

    private ErrorResponse error(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, List.of());
    }
}
