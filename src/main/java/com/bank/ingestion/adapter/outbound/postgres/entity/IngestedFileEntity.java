package com.bank.ingestion.adapter.outbound.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingested_files", schema = "banking")
public class IngestedFileEntity {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "checksum", nullable = false, unique = true, length = 64)
    private String checksum;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "success_rows")
    private Integer successRows;

    @Column(name = "failed_rows")
    private Integer failedRows;

    @Column(name = "uploaded_by", length = 30)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getSuccessRows() { return successRows; }
    public void setSuccessRows(Integer successRows) { this.successRows = successRows; }
    public Integer getFailedRows() { return failedRows; }
    public void setFailedRows(Integer failedRows) { this.failedRows = failedRows; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
