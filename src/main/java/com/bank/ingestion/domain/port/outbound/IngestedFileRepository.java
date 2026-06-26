package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.model.FileStatus;
import com.bank.ingestion.domain.model.IngestedFile;

import java.util.Optional;
import java.util.UUID;

public interface IngestedFileRepository {

    IngestedFile save(IngestedFile ingestedFile);

    Optional<IngestedFile> findById(UUID id);

    boolean existsByChecksum(String checksum);

    void updateStatus(UUID id, FileStatus status);
}
