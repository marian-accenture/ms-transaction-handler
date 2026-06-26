package com.bank.ingestion.application.dto;

import com.bank.ingestion.domain.model.ValidationWarning;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RowProcessingResult {

    private UUID transactionId;
    private boolean success;
    private List<ValidationWarning> warnings = new ArrayList<>();

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<ValidationWarning> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }
}
