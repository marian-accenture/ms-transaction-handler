package com.bank.ingestion.adapter.outbound.postgres;

import com.bank.ingestion.adapter.outbound.postgres.entity.ValidationWarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ValidationWarningJpaRepository extends JpaRepository<ValidationWarningEntity, UUID> {
}
