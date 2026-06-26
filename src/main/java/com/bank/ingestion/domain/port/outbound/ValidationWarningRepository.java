package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.model.ValidationWarning;

import java.util.List;

public interface ValidationWarningRepository {

    ValidationWarning save(ValidationWarning warning);

    List<ValidationWarning> saveAll(List<ValidationWarning> warnings);
}
