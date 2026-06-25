package com.bank.ingestion.adapter.outbound.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
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
    private String currency;

    @ManyToOne
    @JoinColumn(name = "benefactor_id", nullable = false)
    private AccountEntity benefactor;

    @ManyToOne
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private AccountEntity beneficiary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private IngestedFileEntity file;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
