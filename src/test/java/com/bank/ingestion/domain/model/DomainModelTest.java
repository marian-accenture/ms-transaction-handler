package com.bank.ingestion.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    void transactionGettersAndSetters() {
        Transaction tx = new Transaction();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        tx.setId(id);
        tx.setExternalRef("REF-001");
        tx.setTransactionAt(now);
        tx.setType(TransactionType.DEBIT);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setAmountCents(100_00L);
        tx.setCurrency("ARS");
        tx.setFlagged(false);
        tx.setFileId(UUID.randomUUID());

        assertThat(tx.getId()).isEqualTo(id);
        assertThat(tx.getExternalRef()).isEqualTo("REF-001");
        assertThat(tx.getTransactionAt()).isEqualTo(now);
        assertThat(tx.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.getAmountCents()).isEqualTo(100_00L);
        assertThat(tx.getCurrency()).isEqualTo("ARS");
        assertThat(tx.isFlagged()).isFalse();
        assertThat(tx.getFileId()).isNotNull();
    }

    @Test
    void accountGettersAndSetters() {
        Account account = new Account();
        UUID id = UUID.randomUUID();

        account.setId(id);
        account.setCbu("1234567890123456789012");
        account.setCuit("20123456789");
        account.setHolderName("Juan Perez");
        account.setActive(true);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getCbu()).isEqualTo("1234567890123456789012");
        assertThat(account.getCuit()).isEqualTo("20123456789");
        assertThat(account.getHolderName()).isEqualTo("Juan Perez");
        assertThat(account.isActive()).isTrue();
    }

    @Test
    void ingestedFileGettersAndSetters() {
        IngestedFile file = new IngestedFile();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        file.setId(id);
        file.setFileName("transactions.txt");
        file.setFileFormat(FileFormat.TXT);
        file.setStatus(FileStatus.RECEIVED);
        file.setChecksum("abc123");
        file.setFileSizeBytes(1024L);
        file.setUploadedBy("system");
        file.setUploadedAt(now);

        assertThat(file.getId()).isEqualTo(id);
        assertThat(file.getFileName()).isEqualTo("transactions.txt");
        assertThat(file.getFileFormat()).isEqualTo(FileFormat.TXT);
        assertThat(file.getStatus()).isEqualTo(FileStatus.RECEIVED);
        assertThat(file.getChecksum()).isEqualTo("abc123");
        assertThat(file.getFileSizeBytes()).isEqualTo(1024L);
        assertThat(file.getUploadedBy()).isEqualTo("system");
        assertThat(file.getUploadedAt()).isEqualTo(now);
    }

    @Test
    void ingestionSummaryGettersAndSetters() {
        IngestionSummary summary = new IngestionSummary();
        UUID fileId = UUID.randomUUID();
        summary.setFileId(fileId);
        summary.setStatus(FileStatus.COMPLETED);
        summary.setTotalRows(500);
        summary.setSuccessRows(498);
        summary.setFailedRows(2);

        assertThat(summary.getFileId()).isEqualTo(fileId);
        assertThat(summary.getStatus()).isEqualTo(FileStatus.COMPLETED);
        assertThat(summary.getTotalRows()).isEqualTo(500);
        assertThat(summary.getSuccessRows()).isEqualTo(498);
        assertThat(summary.getFailedRows()).isEqualTo(2);
    }

    @Test
    void validationWarningGettersAndSetters() {
        ValidationWarning warning = new ValidationWarning();
        UUID id = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        warning.setId(id);
        warning.setTransactionId(txId);
        warning.setWarningCode("INVALID_CBU");
        warning.setWarningMessage("CBU length must be 22 digits");

        assertThat(warning.getId()).isEqualTo(id);
        assertThat(warning.getTransactionId()).isEqualTo(txId);
        assertThat(warning.getWarningCode()).isEqualTo("INVALID_CBU");
        assertThat(warning.getWarningMessage()).isEqualTo("CBU length must be 22 digits");
    }
}
