package com.payflow.wallet.infrastructure.persistence;

import com.payflow.shared.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 30)
    private String status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletEntity() {
    }

    /**
     * Creates a new wallet entity.
     *
     * <p>Version intentionally starts as null so Spring Data JPA
     * recognizes this entity as new. Hibernate initializes the
     * optimistic-lock version during INSERT.</p>
     */
    public WalletEntity(
            UUID id,
            UUID accountId,
            Currency currency,
            BigDecimal balance
    ) {
        this.id = id;
        this.accountId = accountId;
        this.currency = currency;
        this.balance = balance;
        this.status = "ACTIVE";
        this.version = null;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Creates an entity representing an existing persisted wallet.
     *
     * <p>The supplied version is the version originally read by the
     * domain aggregate. Hibernate uses it for optimistic locking.</p>
     */
    public WalletEntity(
            UUID id,
            UUID accountId,
            Currency currency,
            BigDecimal balance,
            long version,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public void updateBalance(BigDecimal balance) {
        this.balance = balance;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}