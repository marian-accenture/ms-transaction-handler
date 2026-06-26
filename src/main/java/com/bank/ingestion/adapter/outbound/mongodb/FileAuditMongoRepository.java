package com.bank.ingestion.adapter.outbound.mongodb;

import com.bank.ingestion.adapter.outbound.mongodb.document.FileAuditDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileAuditMongoRepository extends MongoRepository<FileAuditDocument, String> {
}
