package com.bank.ingestion.adapter.outbound.mongodb;

import com.bank.ingestion.adapter.outbound.mongodb.document.ProcessingLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessingLogMongoRepository extends MongoRepository<ProcessingLogDocument, String> {
}