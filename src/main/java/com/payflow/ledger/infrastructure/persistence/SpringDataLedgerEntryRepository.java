package com.payflow.ledger.infrastructure.persistence;

import com.payflow.ledger.domain.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataLedgerEntryRepository
        extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByTransactionIdOrderByCreatedAtAsc(
            UUID transactionId
    );

    List<LedgerEntryEntity> findByLedgerAccountIdAndAccountTypeOrderByCreatedAtAsc(
            UUID ledgerAccountId,
            LedgerAccountType accountType
    );
}