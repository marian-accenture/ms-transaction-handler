package com.bank.ingestion.adapter.inbound.kafka;

import com.bank.ingestion.adapter.inbound.kafka.event.FileReadyEvent;
import com.bank.ingestion.domain.exception.DatabaseException;
import com.bank.ingestion.domain.model.FileReadyCommand;
import com.bank.ingestion.domain.port.inbound.FileIngestionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileReadyKafkaListener {

    private final FileIngestionUseCase fileIngestionUseCase;

    @KafkaListener(
            topics = "${app.ingestion.kafka.file-ready-topic}",
            groupId = "${app.ingestion.kafka.consumer-group}",
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void onFileReady(@Payload FileReadyEvent event, Acknowledgment ack) {
        log.info("Received FileReadyEvent: eventId={}, file={}", event.eventId(), event.fileName());
        try {
            fileIngestionUseCase.ingest(toCommand(event));
            ack.acknowledge();
            log.info("Successfully processed file {} (eventId={})", event.fileName(), event.eventId());
        } catch (DatabaseException e) {
            log.error("DB error processing file {} — NOT acknowledging offset for redelivery: {}", event.fileName(), e.getMessage());
            // Do NOT ack — Kafka will redeliver
        } catch (Exception e) {
            log.error("Unexpected error processing file {}: {}", event.fileName(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    private FileReadyCommand toCommand(FileReadyEvent event) {
        return new FileReadyCommand(
                event.eventId(),
                event.filePath(),
                event.fileName(),
                event.fileFormat(),
                event.checksum(),
                event.fileSizeBytes(),
                event.branchCode(),
                event.uploadedBy(),
                event.uploadedAt()
        );
    }
}
