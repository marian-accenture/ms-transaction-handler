package com.bank.ingestion.domain.port.outbound;

import com.bank.ingestion.domain.model.Account;

import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findByCbu(String cbu);

    boolean existsByCbu(String cbu);
}
