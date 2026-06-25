package com.bank.ingestion.adapter.outbound.postgres;

import com.bank.ingestion.adapter.outbound.postgres.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByFileId(UUID fileId);
}
