package com.payflow.ledger.infrastructure.persistence;

import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerEntryType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {
    }

    public LedgerEntryEntity(
            UUID id,
            UUID transactionId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            LedgerEntryType type,
            Instant createdAt
    ) {
        this.id = id;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}