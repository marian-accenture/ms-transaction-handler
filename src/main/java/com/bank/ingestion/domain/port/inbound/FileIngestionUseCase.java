package com.bank.ingestion.domain.port.inbound;

import com.bank.ingestion.domain.model.FileReadyCommand;
import com.bank.ingestion.domain.model.FileStatus;
import com.bank.ingestion.domain.model.IngestedFile;
import com.bank.ingestion.domain.model.PagedResult;
import com.bank.ingestion.domain.model.Transaction;
import com.bank.ingestion.domain.model.ValidationWarning;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FileIngestionUseCase {

    void ingest(FileReadyCommand command);

    PagedResult<IngestedFile> listFiles(FileStatus status, LocalDate from, LocalDate to, String uploadedBy, int page, int size);

    IngestedFile getFile(UUID fileId);

    void retryFile(UUID fileId);

    List<ValidationWarning> getWarnings(UUID fileId, String warningCode);

    PagedResult<Transaction> getTransactions(UUID fileId, String status, Boolean flagged, int page, int size);

    Transaction getTransaction(UUID fileId, UUID transactionId);
}
