package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(AccountId accountId);

    void save(Account account);
}