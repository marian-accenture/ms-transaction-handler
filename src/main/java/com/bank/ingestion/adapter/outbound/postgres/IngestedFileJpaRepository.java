package com.bank.ingestion.adapter.outbound.postgres;

import com.bank.ingestion.adapter.outbound.postgres.entity.IngestedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestedFileJpaRepository extends JpaRepository<IngestedFileEntity, UUID> {

    boolean existsByChecksum(String checksum);
}
