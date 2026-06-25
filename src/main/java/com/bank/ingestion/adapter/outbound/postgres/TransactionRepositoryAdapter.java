package com.bank.ingestion.adapter.outbound.postgres;

import com.bank.ingestion.adapter.outbound.postgres.entity.TransactionEntity;
import com.bank.ingestion.domain.model.Transaction;
import com.bank.ingestion.domain.model.TransactionStatus;
import com.bank.ingestion.domain.model.TransactionType;
import com.bank.ingestion.domain.port.outbound.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
            INSERT INTO banking.transactions
                (id, external_ref, transaction_at, ingested_at, type, status,
                 amount, currency, benefactor_id, beneficiary_id, description,
                 file_id, created_by, flagged)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (external_ref) DO NOTHING
            """;

    @Override
    public void saveAll(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        jdbcTemplate.batchUpdate(INSERT_SQL, transactions, transactions.size(), (ps, tx) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, tx.getExternalRef());
            ps.setTimestamp(3, Timestamp.from(tx.getTransactionAt()));
            ps.setTimestamp(4, Timestamp.from(tx.getIngestedAt()));
            ps.setString(5, tx.getType().name());
            ps.setString(6, tx.getStatus().name());
            ps.setBigDecimal(7, BigDecimal.valueOf(tx.getAmountCents()));
            ps.setString(8, tx.getCurrency());
            ps.setObject(9, tx.getBenefactorId());
            ps.setObject(10, tx.getBeneficiaryId());
            ps.setString(11, tx.getDescription());
            ps.setObject(12, tx.getFileId());
            ps.setString(13, tx.getCreatedBy());
            ps.setBoolean(14, tx.isFlagged());
        });
    }

    @Override
    public List<Transaction> findByFileId(UUID fileId) {
        return jpaRepository.findByFileId(fileId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Transaction toDomain(TransactionEntity e) {
        Transaction t = Transaction.of(
                e.getExternalRef(),
                e.getTransactionAt(),
                e.getIngestedAt(),
                TransactionType.valueOf(e.getType()),
                TransactionStatus.valueOf(e.getStatus()),
                e.getAmount().longValue(),
                e.getCurrency(),
                e.getBenefactor().getId(),
                e.getBeneficiary().getId(),
                e.getDescription(),
                e.getFile().getId(),
                e.getCreatedBy(),
                e.isFlagged()
        );
        t.setId(e.getId());
        return t;
    }
}
