package com.bank.ingestion.application.dto;

public class ParsedRow {

    private String recType;
    private String externalRef;
    private String transactionAt;
    private String type;
    private String status;
    private String amountCents;
    private String currency;
    private String benefactorCbu;
    private String benefactorCuit;
    private String benefactorName;
    private String beneficiaryCbu;
    private String beneficiaryCuit;
    private String beneficiaryName;
    private String description;
    private String flagged;

    public String getRecType() { return recType; }
    public void setRecType(String recType) { this.recType = recType; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public String getTransactionAt() { return transactionAt; }
    public void setTransactionAt(String transactionAt) { this.transactionAt = transactionAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAmountCents() { return amountCents; }
    public void setAmountCents(String amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBenefactorCbu() { return benefactorCbu; }
    public void setBenefactorCbu(String benefactorCbu) { this.benefactorCbu = benefactorCbu; }

    public String getBenefactorCuit() { return benefactorCuit; }
    public void setBenefactorCuit(String benefactorCuit) { this.benefactorCuit = benefactorCuit; }

    public String getBenefactorName() { return benefactorName; }
    public void setBenefactorName(String benefactorName) { this.benefactorName = benefactorName; }

    public String getBeneficiaryCbu() { return beneficiaryCbu; }
    public void setBeneficiaryCbu(String beneficiaryCbu) { this.beneficiaryCbu = beneficiaryCbu; }

    public String getBeneficiaryCuit() { return beneficiaryCuit; }
    public void setBeneficiaryCuit(String beneficiaryCuit) { this.beneficiaryCuit = beneficiaryCuit; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFlagged() { return flagged; }
    public void setFlagged(String flagged) { this.flagged = flagged; }
}
