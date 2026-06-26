package com.bank.ingestion.adapter.outbound.postgres;

import com.bank.ingestion.adapter.outbound.postgres.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByCbu(String cbu);

    boolean existsByCbu(String cbu);
}
