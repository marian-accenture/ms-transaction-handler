package com.bank.ingestion.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainServiceTest {

    @Test
    void ingestionOrchestratorCanBeInstantiated() {
        assertThat(new IngestionOrchestrator()).isNotNull();
    }

    @Test
    void fileValidationServiceCanBeInstantiated() {
        assertThat(new FileValidationService()).isNotNull();
    }

    @Test
    void rowValidationServiceCanBeInstantiated() {
        assertThat(new RowValidationService()).isNotNull();
    }

    @Test
    void accountResolutionServiceCanBeInstantiated() {
        assertThat(new AccountResolutionService()).isNotNull();
    }
}
