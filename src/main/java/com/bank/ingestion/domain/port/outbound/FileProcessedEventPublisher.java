package com.bank.ingestion.domain.port.outbound;

import java.util.UUID;

public interface FileProcessedEventPublisher {

    void publish(UUID fileId, String status, int successRows, int failedRows);
}
