package com.bank.ingestion.application.dto;

import com.bank.ingestion.domain.model.FileStatus;

import java.util.UUID;

public record FileIngestionResult(
        UUID fileId,
        FileStatus status,
        int totalRows,
        int successRows,
        int failedRows,
        int flaggedCount,
        String errorMessage
) {
    public static FileIngestionResult completed(UUID fileId, int total, int success, int failed, int flagged) {
        FileStatus status = (success == 0 && failed > 0) ? FileStatus.FAILED : FileStatus.COMPLETED;
        return new FileIngestionResult(fileId, status, total, success, failed, flagged, null);
    }

    public static FileIngestionResult failed(UUID fileId, String errorMessage) {
        return new FileIngestionResult(fileId, FileStatus.FAILED, 0, 0, 0, 0, errorMessage);
    }

    public static FileIngestionResult duplicate(UUID fileId) {
        return new FileIngestionResult(fileId, FileStatus.DUPLICATE, 0, 0, 0, 0, null);
    }
}
