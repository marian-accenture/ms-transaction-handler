package com.bank.ingestion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class IngestedFile {

    private UUID id;
    private String fileName;
    private FileFormat fileFormat;
    private FileStatus status;
    private String checksum;
    private long fileSizeBytes;
    private String uploadedBy;
    private Instant uploadedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public FileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(FileFormat fileFormat) { this.fileFormat = fileFormat; }

    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
