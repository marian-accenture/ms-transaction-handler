package com.bank.ingestion.adapter.outbound.kafka;

import com.bank.ingestion.domain.port.outbound.FileProcessedEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class FileProcessedEventKafkaPublisher implements FileProcessedEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public FileProcessedEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                            @Value("${app.ingestion.kafka.file-processed-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(UUID fileId, String status, int successRows, int failedRows) {
        Map<String, Object> event = Map.of(
                "fileId", fileId.toString(),
                "status", status,
                "successRows", successRows,
                "failedRows", failedRows
        );
        kafkaTemplate.send(topic, fileId.toString(), event);
    }
}
