package com.bank.ingestion.adapter.outbound.postgres.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", schema = "banking")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_ref", nullable = false, unique = true, length = 30)
    private String externalRef;

    @Column(name = "transaction_at", nullable = false)
    private Instant transactionAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefactor_id", nullable = false)
    private AccountEntity benefactor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private AccountEntity beneficiary;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private IngestedFileEntity file;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public Instant getTransactionAt() { return transactionAt; }
    public void setTransactionAt(Instant transactionAt) { this.transactionAt = transactionAt; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public AccountEntity getBenefactor() { return benefactor; }
    public void setBenefactor(AccountEntity benefactor) { this.benefactor = benefactor; }
    public AccountEntity getBeneficiary() { return beneficiary; }
    public void setBeneficiary(AccountEntity beneficiary) { this.beneficiary = beneficiary; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    public IngestedFileEntity getFile() { return file; }
    public void setFile(IngestedFileEntity file) { this.file = file; }
}
