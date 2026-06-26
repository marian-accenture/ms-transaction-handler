package com.bank.ingestion.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Transaction {

    private UUID id;
    private String externalRef;
    private Instant transactionAt;
    private TransactionType type;
    private TransactionStatus status;
    private long amountCents;
    private String currency;
    private boolean flagged;
    private UUID fileId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public Instant getTransactionAt() { return transactionAt; }
    public void setTransactionAt(Instant transactionAt) { this.transactionAt = transactionAt; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public long getAmountCents() { return amountCents; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public UUID getFileId() { return fileId; }
    public void setFileId(UUID fileId) { this.fileId = fileId; }
}
