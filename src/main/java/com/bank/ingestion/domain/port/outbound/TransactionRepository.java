package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> saveAll(List<Transaction> transactions);

    boolean existsByExternalRef(String externalRef);
}
