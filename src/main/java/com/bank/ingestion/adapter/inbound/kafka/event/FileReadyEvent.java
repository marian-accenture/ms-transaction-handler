package com.bank.ingestion.adapter.inbound.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record FileReadyEvent(
        UUID eventId,
        String filePath,
        String fileName,
        String fileFormat,
        String checksum,
        long fileSizeBytes,
        String branchCode,
        String uploadedBy,
        Instant uploadedAt
) {}
