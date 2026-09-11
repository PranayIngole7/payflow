package com.payflow.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataWalletRepository
        extends JpaRepository<WalletEntity, UUID> {

    Optional<WalletEntity> findByAccountId(UUID accountId);
}