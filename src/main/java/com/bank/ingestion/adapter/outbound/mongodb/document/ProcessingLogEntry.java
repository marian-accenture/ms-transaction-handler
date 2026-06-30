package com.bank.ingestion.adapter.outbound.mongodb.document;

import java.time.Instant;

public class ProcessingLogEntry {

    private Instant timestamp;
    private String level;
    private String phase;
    private String message;

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}