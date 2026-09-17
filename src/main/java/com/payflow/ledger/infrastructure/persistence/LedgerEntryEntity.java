package com.payflow.ledger.infrastructure.persistence;

import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "ledger_account_id", nullable = false)
    private UUID ledgerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private LedgerAccountType accountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private LedgerEntryType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {
    }

    public LedgerEntryEntity(
            UUID id,
            UUID transactionId,
            UUID ledgerAccountId,
            LedgerAccountType accountType,
            BigDecimal amount,
            Currency currency,
            LedgerEntryType type,
            Instant createdAt
    ) {
        this.id = id;
        this.transactionId = transactionId;
        this.ledgerAccountId = ledgerAccountId;
        this.accountType = accountType;
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

    public UUID getLedgerAccountId() {
        return ledgerAccountId;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
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
