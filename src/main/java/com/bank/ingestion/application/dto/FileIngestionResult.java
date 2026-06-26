package com.bank.ingestion.application.dto;

import com.bank.ingestion.domain.model.FileStatus;

import java.util.UUID;

public class FileIngestionResult {

    private UUID fileId;
    private FileStatus status;
    private int totalRows;
    private int successRows;
    private int failedRows;

    public UUID getFileId() { return fileId; }
    public void setFileId(UUID fileId) { this.fileId = fileId; }

    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getSuccessRows() { return successRows; }
    public void setSuccessRows(int successRows) { this.successRows = successRows; }

    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int failedRows) { this.failedRows = failedRows; }
}
