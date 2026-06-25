package com.bank.ingestion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Transaction {

    private UUID id;
    private String externalRef;
    private Instant transactionAt;
    private Instant ingestedAt;
    private TransactionType type;
    private TransactionStatus status;
    private long amountCents;
    private String currency;
    private UUID benefactorId;
    private UUID beneficiaryId;
    private String description;
    private UUID fileId;
    private String createdBy;
    private boolean flagged;
    private String flagReason;

    private Transaction() {}

    public static Transaction of(
            String externalRef,
            Instant transactionAt,
            Instant ingestedAt,
            TransactionType type,
            TransactionStatus status,
            long amountCents,
            String currency,
            UUID benefactorId,
            UUID beneficiaryId,
            String description,
            UUID fileId,
            String createdBy,
            boolean flagged
    ) {
        Transaction t = new Transaction();
        t.externalRef = externalRef;
        t.transactionAt = transactionAt;
        t.ingestedAt = ingestedAt;
        t.type = type;
        t.status = status;
        t.amountCents = amountCents;
        t.currency = currency;
        t.benefactorId = benefactorId;
        t.beneficiaryId = beneficiaryId;
        t.description = description;
        t.fileId = fileId;
        t.createdBy = createdBy;
        t.flagged = flagged;
        return t;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalRef() { return externalRef; }
    public Instant getTransactionAt() { return transactionAt; }
    public Instant getIngestedAt() { return ingestedAt; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public UUID getBenefactorId() { return benefactorId; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public String getDescription() { return description; }
    public UUID getFileId() { return fileId; }
    public String getCreatedBy() { return createdBy; }
    public boolean isFlagged() { return flagged; }
    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
}
