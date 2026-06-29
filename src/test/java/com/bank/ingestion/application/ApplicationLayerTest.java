package com.bank.ingestion.application;

import com.bank.ingestion.application.dto.FileIngestionResult;
import com.bank.ingestion.application.dto.ParsedRow;
import com.bank.ingestion.application.dto.RowProcessingResult;
import com.bank.ingestion.application.usecase.FileIngestionUseCaseImpl;
import com.bank.ingestion.domain.model.FileStatus;
import com.bank.ingestion.domain.port.outbound.FileParserResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationLayerTest {

    @Test
    void parsedRowGettersAndSetters() {
        ParsedRow row = new ParsedRow();
        row.setRecType("D");
        row.setExternalRef("EXT-001");
        row.setAmountCents("10000");
        row.setCurrency("ARS");
        row.setBenefactorCbu("1234567890123456789012");
        row.setBeneficiaryCbu("2234567890123456789012");

        assertThat(row.getRecType()).isEqualTo("D");
        assertThat(row.getExternalRef()).isEqualTo("EXT-001");
        assertThat(row.getAmountCents()).isEqualTo("10000");
        assertThat(row.getCurrency()).isEqualTo("ARS");
        assertThat(row.getBenefactorCbu()).isEqualTo("1234567890123456789012");
        assertThat(row.getBeneficiaryCbu()).isEqualTo("2234567890123456789012");
    }

    @Test
    void parsedRowRemainingFields() {
        ParsedRow row = new ParsedRow();
        row.setTransactionAt("20240101120000");
        row.setType("DEBIT");
        row.setStatus("PENDING");
        row.setBenefactorCuit("20123456789");
        row.setBenefactorName("Sender Name");
        row.setBeneficiaryCuit("20987654321");
        row.setBeneficiaryName("Receiver Name");
        row.setDescription("Payment for services");
        row.setFlagged("0");

        assertThat(row.getTransactionAt()).isEqualTo("20240101120000");
        assertThat(row.getType()).isEqualTo("DEBIT");
        assertThat(row.getStatus()).isEqualTo("PENDING");
        assertThat(row.getBenefactorCuit()).isEqualTo("20123456789");
        assertThat(row.getBenefactorName()).isEqualTo("Sender Name");
        assertThat(row.getBeneficiaryCuit()).isEqualTo("20987654321");
        assertThat(row.getBeneficiaryName()).isEqualTo("Receiver Name");
        assertThat(row.getDescription()).isEqualTo("Payment for services");
        assertThat(row.getFlagged()).isEqualTo("0");
    }

    @Test
    void fileIngestionResultGettersAndSetters() {
        FileIngestionResult result = new FileIngestionResult();
        UUID fileId = UUID.randomUUID();
        result.setFileId(fileId);
        result.setStatus(FileStatus.COMPLETED);
        result.setTotalRows(100);
        result.setSuccessRows(98);
        result.setFailedRows(2);

        assertThat(result.getFileId()).isEqualTo(fileId);
        assertThat(result.getStatus()).isEqualTo(FileStatus.COMPLETED);
        assertThat(result.getTotalRows()).isEqualTo(100);
        assertThat(result.getSuccessRows()).isEqualTo(98);
        assertThat(result.getFailedRows()).isEqualTo(2);
    }

    @Test
    void rowProcessingResultGettersAndSetters() {
        RowProcessingResult result = new RowProcessingResult();
        UUID txId = UUID.randomUUID();
        result.setTransactionId(txId);
        result.setSuccess(true);

        assertThat(result.getTransactionId()).isEqualTo(txId);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void fileIngestionUseCaseImplCanBeInstantiated() {
        FileParserResolver fileParserResolver = new FileParserResolver() {
            @Override
            public void ensureSupported(Path filePath) {
                // no-op for constructor test
            }
        };
        assertThat(new FileIngestionUseCaseImpl(fileParserResolver)).isNotNull();
    }
}

