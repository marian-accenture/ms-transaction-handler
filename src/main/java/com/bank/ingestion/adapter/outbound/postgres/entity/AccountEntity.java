package com.bank.ingestion.adapter.outbound.postgres.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "banking")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

    @Column(name = "cbu", nullable = false, unique = true, length = 22)
    private String cbu;

    @Column(name = "cuit", nullable = false, unique = true, length = 11)
    private String cuit;

    @Column(name = "holder_name", nullable = false, length = 40)
    private String holderName;

    @Column(name = "holder_type", length = 10)
    private String holderType;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "default_currency", length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String defaultCurrency;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCbu() { return cbu; }
    public void setCbu(String cbu) { this.cbu = cbu; }
    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
