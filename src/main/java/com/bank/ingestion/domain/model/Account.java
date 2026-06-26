package com.bank.ingestion.domain.model;

import java.util.UUID;

public class Account {

    private UUID id;
    private String cbu;
    private String cuit;
    private String holderName;
    private boolean active;

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
}
