package com.bank.ingestion.adapter.outbound.mongodb;

import com.bank.ingestion.adapter.outbound.mongodb.document.FileAuditDocument;
import com.bank.ingestion.adapter.outbound.mongodb.document.ProcessingLogDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
@Import(FileAuditMongoAdapter.class)
class MongoAuditEmbeddedIntegrationTest {

    @Autowired
    private FileAuditMongoAdapter fileAuditMongoAdapter;

    @Autowired
    private FileAuditMongoRepository fileAuditMongoRepository;

    @Autowired
    private ProcessingLogMongoRepository processingLogMongoRepository;

    @BeforeEach
    void setUp() {
        processingLogMongoRepository.deleteAll();
        fileAuditMongoRepository.deleteAll();
    }

    @Test
    void processingStartAndCompletePersistAuditAndInfoLog() {
        UUID fileId = UUID.randomUUID();

        fileAuditMongoAdapter.logProcessingStart(fileId, "transactions.json");
        fileAuditMongoAdapter.logProcessingComplete(fileId, 10, 2);

        FileAuditDocument audit = fileAuditMongoRepository.findById(fileId.toString()).orElseThrow();
        ProcessingLogDocument log = processingLogMongoRepository.findById(fileId.toString()).orElseThrow();

        assertThat(audit.getFileName()).isEqualTo("transactions.json");
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");
        assertThat(audit.getSuccessRows()).isEqualTo(10);
        assertThat(audit.getFailedRows()).isEqualTo(2);
        assertThat(audit.getStartedAt()).isNotNull();
        assertThat(audit.getCompletedAt()).isNotNull();

        assertThat(log.getEntries()).hasSize(2);
        assertThat(log.getEntries().get(0).getLevel()).isEqualTo("INFO");
        assertThat(log.getEntries().get(0).getPhase()).isEqualTo("INGEST");
        assertThat(log.getEntries().get(0).getMessage()).contains("Processing started");
        assertThat(log.getEntries().get(1).getLevel()).isEqualTo("INFO");
        assertThat(log.getEntries().get(1).getMessage()).contains("Processing completed");

        writeEvidenceFile("mongodb-audit-embedded-supported.json", toEvidenceJson(audit, log));
    }

    @Test
    void processingErrorPersistsFailureAuditAndErrorLog() {
        UUID fileId = UUID.randomUUID();

        fileAuditMongoAdapter.logProcessingStart(fileId, "transactions.xml");
        fileAuditMongoAdapter.logProcessingError(fileId, "Unsupported file format for file: transactions.xml");

        FileAuditDocument audit = fileAuditMongoRepository.findById(fileId.toString()).orElseThrow();
        ProcessingLogDocument log = processingLogMongoRepository.findById(fileId.toString()).orElseThrow();

        assertThat(audit.getFileName()).isEqualTo("transactions.xml");
        assertThat(audit.getStatus()).isEqualTo("FAILED");
        assertThat(audit.getErrorMessage()).isEqualTo("Unsupported file format for file: transactions.xml");
        assertThat(audit.getStartedAt()).isNotNull();
        assertThat(audit.getCompletedAt()).isNotNull();

        assertThat(log.getEntries()).hasSize(2);
        assertThat(log.getEntries().get(0).getLevel()).isEqualTo("INFO");
        assertThat(log.getEntries().get(1).getLevel()).isEqualTo("ERROR");
        assertThat(log.getEntries().get(1).getMessage()).isEqualTo("Unsupported file format for file: transactions.xml");

        writeEvidenceFile("mongodb-audit-embedded-unsupported.json", toEvidenceJson(audit, log));
    }

    private static String toEvidenceJson(FileAuditDocument audit, ProcessingLogDocument log) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"fileUpload\": {\n");
        sb.append("    \"id\": \"").append(audit.getId()).append("\",\n");
        sb.append("    \"fileName\": \"").append(audit.getFileName()).append("\",\n");
        sb.append("    \"status\": \"").append(audit.getStatus()).append("\",\n");
        sb.append("    \"successRows\": ").append(audit.getSuccessRows()).append(",\n");
        sb.append("    \"failedRows\": ").append(audit.getFailedRows()).append(",\n");
        sb.append("    \"errorMessage\": ").append(audit.getErrorMessage() == null ? "null" : "\"" + audit.getErrorMessage() + "\"").append(",\n");
        sb.append("    \"startedAt\": \"").append(audit.getStartedAt()).append("\",\n");
        sb.append("    \"completedAt\": ").append(audit.getCompletedAt() == null ? "null" : "\"" + audit.getCompletedAt() + "\"").append("\n");
        sb.append("  },\n");
        sb.append("  \"processingLogs\": [\n");
        for (int i = 0; i < log.getEntries().size(); i++) {
            var entry = log.getEntries().get(i);
            sb.append("    {\n");
            sb.append("      \"timestamp\": \"").append(entry.getTimestamp()).append("\",\n");
            sb.append("      \"level\": \"").append(entry.getLevel()).append("\",\n");
            sb.append("      \"phase\": \"").append(entry.getPhase()).append("\",\n");
            sb.append("      \"message\": \"").append(entry.getMessage()).append("\"\n");
            sb.append("    }");
            if (i < log.getEntries().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void writeEvidenceFile(String fileName, String content) {
        try {
            Path evidenceDir = Path.of("target", "evidence");
            Files.createDirectories(evidenceDir);
            Files.writeString(evidenceDir.resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write Mongo embedded evidence file", ex);
        }
    }
}