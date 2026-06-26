package com.bank.ingestion.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumsTest {

    @Test
    void transactionTypeContainsAllValues() {
        assertThat(TransactionType.values())
                .containsExactly(
                        TransactionType.DEBIT,
                        TransactionType.CREDIT,
                        TransactionType.TRANSFER,
                        TransactionType.PAYMENT,
                        TransactionType.REFUND,
                        TransactionType.FEE
                );
    }

    @Test
    void transactionTypeValueOf() {
        assertThat(TransactionType.valueOf("DEBIT")).isEqualTo(TransactionType.DEBIT);
        assertThat(TransactionType.valueOf("CREDIT")).isEqualTo(TransactionType.CREDIT);
        assertThat(TransactionType.valueOf("TRANSFER")).isEqualTo(TransactionType.TRANSFER);
        assertThat(TransactionType.valueOf("PAYMENT")).isEqualTo(TransactionType.PAYMENT);
        assertThat(TransactionType.valueOf("REFUND")).isEqualTo(TransactionType.REFUND);
        assertThat(TransactionType.valueOf("FEE")).isEqualTo(TransactionType.FEE);
    }

    @Test
    void transactionStatusContainsAllValues() {
        assertThat(TransactionStatus.values())
                .containsExactly(
                        TransactionStatus.PENDING,
                        TransactionStatus.COMPLETED,
                        TransactionStatus.FAILED,
                        TransactionStatus.REVERSED
                );
    }

    @Test
    void transactionStatusValueOf() {
        assertThat(TransactionStatus.valueOf("PENDING")).isEqualTo(TransactionStatus.PENDING);
        assertThat(TransactionStatus.valueOf("COMPLETED")).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(TransactionStatus.valueOf("FAILED")).isEqualTo(TransactionStatus.FAILED);
        assertThat(TransactionStatus.valueOf("REVERSED")).isEqualTo(TransactionStatus.REVERSED);
    }

    @Test
    void fileStatusContainsAllValues() {
        assertThat(FileStatus.values())
                .containsExactly(
                        FileStatus.RECEIVED,
                        FileStatus.PROCESSING,
                        FileStatus.COMPLETED,
                        FileStatus.FAILED,
                        FileStatus.DUPLICATE
                );
    }

    @Test
    void fileStatusValueOf() {
        assertThat(FileStatus.valueOf("RECEIVED")).isEqualTo(FileStatus.RECEIVED);
        assertThat(FileStatus.valueOf("PROCESSING")).isEqualTo(FileStatus.PROCESSING);
        assertThat(FileStatus.valueOf("COMPLETED")).isEqualTo(FileStatus.COMPLETED);
        assertThat(FileStatus.valueOf("FAILED")).isEqualTo(FileStatus.FAILED);
        assertThat(FileStatus.valueOf("DUPLICATE")).isEqualTo(FileStatus.DUPLICATE);
    }

    @Test
    void fileFormatContainsAllValues() {
        assertThat(FileFormat.values())
                .containsExactly(
                        FileFormat.TXT,
                        FileFormat.CSV,
                        FileFormat.JSON,
                        FileFormat.XML
                );
    }

    @Test
    void fileFormatValueOf() {
        assertThat(FileFormat.valueOf("TXT")).isEqualTo(FileFormat.TXT);
        assertThat(FileFormat.valueOf("CSV")).isEqualTo(FileFormat.CSV);
        assertThat(FileFormat.valueOf("JSON")).isEqualTo(FileFormat.JSON);
        assertThat(FileFormat.valueOf("XML")).isEqualTo(FileFormat.XML);
    }
}
