package com.payflow.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataWalletRepository
        extends JpaRepository<WalletEntity, UUID> {
}
