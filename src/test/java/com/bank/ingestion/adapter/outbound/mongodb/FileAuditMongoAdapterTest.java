package com.bank.ingestion.adapter.outbound.mongodb;

import com.bank.ingestion.adapter.outbound.mongodb.document.FileAuditDocument;
import com.bank.ingestion.adapter.outbound.mongodb.document.ProcessingLogDocument;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileAuditMongoAdapterTest {

    @Test
    void logProcessingStartCreatesDocumentWithProcessingStatus() {
        FileAuditMongoRepository fileUploadRepository = mock(FileAuditMongoRepository.class);
        ProcessingLogMongoRepository processingLogRepository = mock(ProcessingLogMongoRepository.class);
        when(fileUploadRepository.findById(any())).thenReturn(Optional.empty());
        when(processingLogRepository.findById(any())).thenReturn(Optional.empty());
        when(fileUploadRepository.save(any(FileAuditDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processingLogRepository.save(any(ProcessingLogDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileAuditMongoAdapter adapter = new FileAuditMongoAdapter(fileUploadRepository, processingLogRepository);
        UUID fileId = UUID.randomUUID();

        adapter.logProcessingStart(fileId, "transactions.json");

        verify(fileUploadRepository).save(any(FileAuditDocument.class));
        verify(processingLogRepository).save(any(ProcessingLogDocument.class));
    }

    @Test
    void logProcessingCompleteUpdatesExistingDocument() {
        FileAuditMongoRepository fileUploadRepository = mock(FileAuditMongoRepository.class);
        ProcessingLogMongoRepository processingLogRepository = mock(ProcessingLogMongoRepository.class);
        FileAuditDocument existing = new FileAuditDocument();
        existing.setId(UUID.randomUUID().toString());
        when(fileUploadRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(fileUploadRepository.save(any(FileAuditDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processingLogRepository.findById(existing.getId())).thenReturn(Optional.of(new ProcessingLogDocument()));
        when(processingLogRepository.save(any(ProcessingLogDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileAuditMongoAdapter adapter = new FileAuditMongoAdapter(fileUploadRepository, processingLogRepository);

        adapter.logProcessingComplete(UUID.fromString(existing.getId()), 10, 2);

        assertThat(existing.getStatus()).isEqualTo("COMPLETED");
        assertThat(existing.getSuccessRows()).isEqualTo(10);
        assertThat(existing.getFailedRows()).isEqualTo(2);
        assertThat(existing.getCompletedAt()).isNotNull();
        verify(fileUploadRepository).save(existing);
        verify(processingLogRepository).save(any(ProcessingLogDocument.class));
    }

    @Test
    void logProcessingErrorStoresFailureMessage() {
        FileAuditMongoRepository fileUploadRepository = mock(FileAuditMongoRepository.class);
        ProcessingLogMongoRepository processingLogRepository = mock(ProcessingLogMongoRepository.class);
        when(fileUploadRepository.findById(any())).thenReturn(Optional.empty());
        when(processingLogRepository.findById(any())).thenReturn(Optional.empty());
        when(fileUploadRepository.save(any(FileAuditDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processingLogRepository.save(any(ProcessingLogDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileAuditMongoAdapter adapter = new FileAuditMongoAdapter(fileUploadRepository, processingLogRepository);
        UUID fileId = UUID.randomUUID();

        adapter.logProcessingError(fileId, "Parser failed");

        verify(fileUploadRepository).save(any(FileAuditDocument.class));
        verify(processingLogRepository).save(any(ProcessingLogDocument.class));
    }
}