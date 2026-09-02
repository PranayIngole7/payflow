package com.payflow.transaction.infrastructure.persistence;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransactionRepositoryAdapterTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SpringDataTransactionRepository springDataRepository;

    @Test
    void shouldSaveAndLoadPendingTransaction() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T10:00:00Z"
        );

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("125.50"),
                        Currency.INR
                ),
                createdAt
        );

        transactionRepository.save(transaction);

        Optional<Transaction> result =
                transactionRepository.findById(transactionId);

        assertThat(result).isPresent();

        Transaction loaded = result.orElseThrow();

        assertThat(loaded.id()).isEqualTo(transactionId);
        assertThat(loaded.sourceWalletId())
                .isEqualTo(sourceWalletId);
        assertThat(loaded.destinationWalletId())
                .isEqualTo(destinationWalletId);
        assertThat(loaded.amount().amount())
                .isEqualByComparingTo("125.50");
        assertThat(loaded.currency())
                .isEqualTo(Currency.INR);
        assertThat(loaded.createdAt())
                .isEqualTo(createdAt);
        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldPreserveCompletedTransactionStatus() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                Instant.parse("2026-09-02T10:15:00Z")
        );

        transaction.complete();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldPreserveFailedTransactionStatus() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("75.25"),
                        Currency.INR
                ),
                Instant.parse("2026-09-02T10:30:00Z")
        );

        transaction.fail();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void shouldReturnEmptyWhenTransactionDoesNotExist() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        Optional<Transaction> result =
                transactionRepository.findById(transactionId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldPersistExpectedEntityValues() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T11:00:00Z"
        );

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("999.99"),
                        Currency.INR
                ),
                createdAt
        );

        transactionRepository.save(transaction);

        TransactionEntity entity =
                springDataRepository.findById(transactionId.value())
                        .orElseThrow();

        assertThat(entity.getId())
                .isEqualTo(transactionId.value());

        assertThat(entity.getSourceWalletId())
                .isEqualTo(sourceWalletId.value());

        assertThat(entity.getDestinationWalletId())
                .isEqualTo(destinationWalletId.value());

        assertThat(entity.getAmount())
                .isEqualByComparingTo("999.99");

        assertThat(entity.getCurrency())
                .isEqualTo(Currency.INR);

        assertThat(entity.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(entity.getStatus())
                .isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldRoundTripTransactionWithoutChangingDomainState() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T12:00:00Z"
        );

        Transaction original = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("2500.00"),
                        Currency.INR
                ),
                createdAt
        );

        original.complete();

        transactionRepository.save(original);

        Transaction restored =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertThat(restored.id())
                .isEqualTo(original.id());
        assertThat(restored.sourceWalletId())
                .isEqualTo(original.sourceWalletId());
        assertThat(restored.destinationWalletId())
                .isEqualTo(original.destinationWalletId());
        assertThat(restored.amount())
                .isEqualTo(original.amount());
        assertThat(restored.currency())
                .isEqualTo(original.currency());
        assertThat(restored.createdAt())
                .isEqualTo(original.createdAt());
        assertThat(restored.status())
                .isEqualTo(original.status());
    }
}