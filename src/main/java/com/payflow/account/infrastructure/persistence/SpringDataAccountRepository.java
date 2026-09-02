package com.payflow.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAccountRepository
        extends JpaRepository<AccountEntity, UUID> {
}