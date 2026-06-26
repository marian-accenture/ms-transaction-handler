package com.bank.ingestion.adapter.inbound.kafka;

import com.bank.ingestion.domain.port.inbound.FileIngestionUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class FileReadyKafkaListener {

    private final FileIngestionUseCase fileIngestionUseCase;

    public FileReadyKafkaListener(FileIngestionUseCase fileIngestionUseCase) {
        this.fileIngestionUseCase = fileIngestionUseCase;
    }

    @KafkaListener(
            topics = "${app.ingestion.kafka.file-ready-topic}",
            groupId = "${app.ingestion.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, FileReadyEvent> record, Acknowledgment acknowledgment) {
        // TODO: parse event, delegate to use case, ack on success
        throw new UnsupportedOperationException("TODO: implement listener");
    }
}
