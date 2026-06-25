package com.bank.ingestion.domain.service;

import com.bank.ingestion.domain.model.Account;
import com.bank.ingestion.domain.port.outbound.AccountRepository;

import java.util.UUID;

public class AccountResolutionService {

    private final AccountRepository accountRepository;

    public AccountResolutionService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public UUID resolveOrCreate(String cbu, String cuit, String holderName) {
        return accountRepository.findByCbu(cbu)
                .map(Account::getId)
                .orElseGet(() -> {
                    Account newAccount = Account.newFrom(cbu, cuit, holderName);
                    return accountRepository.save(newAccount).getId();
                });
    }
}
