package com.bank.ingestion.adapter.outbound.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "processing_logs")
public class ProcessingLogDocument {

    @Id
    private String id;

    private List<ProcessingLogEntry> entries = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<ProcessingLogEntry> getEntries() { return entries; }
    public void setEntries(List<ProcessingLogEntry> entries) { this.entries = entries; }
}