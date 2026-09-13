package com.payflow.transaction.infrastructure.persistence;

import com.payflow.account.domain.AccountStatus;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
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

    @Autowired
    private SpringDataAccountRepository accountRepository;

    @Autowired
    private SpringDataWalletRepository walletRepository;

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

        createWalletFixtures(
                sourceWalletId,
                destinationWalletId
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
                transactionRepository.findById(
                        transactionId
                );

        assertThat(result).isPresent();

        Transaction loaded =
                result.orElseThrow();

        assertThat(loaded.id())
                .isEqualTo(transactionId);

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

        assertThat(loaded.type())
                .isEqualTo(TransactionType.TRANSFER);

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

        createWalletFixtures(
                sourceWalletId,
                destinationWalletId
        );

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                Instant.parse(
                        "2026-09-02T10:15:00Z"
                )
        );

        transaction.complete();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(
                        transactionId
                ).orElseThrow();

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.COMPLETED);

        assertThat(loaded.type())
                .isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void shouldPreserveFailedTransactionStatus() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        createWalletFixtures(
                sourceWalletId,
                destinationWalletId
        );

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("75.25"),
                        Currency.INR
                ),
                Instant.parse(
                        "2026-09-02T10:30:00Z"
                )
        );

        transaction.fail();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(
                        transactionId
                ).orElseThrow();

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.FAILED);

        assertThat(loaded.type())
                .isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void shouldReturnEmptyWhenTransactionDoesNotExist() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        Optional<Transaction> result =
                transactionRepository.findById(
                        transactionId
                );

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

        createWalletFixtures(
                sourceWalletId,
                destinationWalletId
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
                springDataRepository
                        .findById(transactionId.value())
                        .orElseThrow();

        assertThat(entity.getId())
                .isEqualTo(transactionId.value());

        assertThat(entity.getSourceWalletId())
                .isEqualTo(sourceWalletId.value());

        assertThat(entity.getDestinationWalletId())
                .isEqualTo(destinationWalletId.value());

        assertThat(entity.getType())
                .isEqualTo("TRANSFER");

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

        createWalletFixtures(
                sourceWalletId,
                destinationWalletId
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
                transactionRepository.findById(
                        transactionId
                ).orElseThrow();

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

        assertThat(restored.type())
                .isEqualTo(original.type());

        assertThat(restored.status())
                .isEqualTo(original.status());
    }

    @Test
    void shouldSaveAndLoadDepositTransaction() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId walletId =
                new WalletId(UUID.randomUUID());

        WalletId unusedWalletId =
                new WalletId(UUID.randomUUID());

        createWalletFixtures(
                walletId,
                unusedWalletId
        );

        Instant createdAt = Instant.parse(
                "2026-09-02T13:00:00Z"
        );

        Transaction transaction =
                Transaction.createDeposit(
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("250.00"),
                                Currency.INR
                        ),
                        createdAt,
                        "deposit-test"
                );

        transaction.complete();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(
                        transactionId
                ).orElseThrow();

        assertThat(loaded.id())
                .isEqualTo(transactionId);

        assertThat(loaded.type())
                .isEqualTo(TransactionType.DEPOSIT);

        assertThat(loaded.sourceWalletId())
                .isNull();

        assertThat(loaded.destinationWalletId())
                .isEqualTo(walletId);

        assertThat(loaded.amount().amount())
                .isEqualByComparingTo("250.00");

        assertThat(loaded.currency())
                .isEqualTo(Currency.INR);

        assertThat(loaded.createdAt())
                .isEqualTo(createdAt);

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldPersistDepositWithNullableSourceWallet() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId walletId =
                new WalletId(UUID.randomUUID());

        WalletId unusedWalletId =
                new WalletId(UUID.randomUUID());

        createWalletFixtures(
                walletId,
                unusedWalletId
        );

        Transaction transaction =
                Transaction.createDeposit(
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("300.00"),
                                Currency.INR
                        ),
                        Instant.parse(
                                "2026-09-02T13:30:00Z"
                        ),
                        "deposit-persistence-test"
                );

        transaction.complete();

        transactionRepository.save(transaction);

        TransactionEntity entity =
                springDataRepository
                        .findById(transactionId.value())
                        .orElseThrow();

        assertThat(entity.getType())
                .isEqualTo("DEPOSIT");

        assertThat(entity.getSourceWalletId())
                .isNull();

        assertThat(entity.getDestinationWalletId())
                .isEqualTo(walletId.value());

        assertThat(entity.getAmount())
                .isEqualByComparingTo("300.00");

        assertThat(entity.getCurrency())
                .isEqualTo(Currency.INR);

        assertThat(entity.getStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldSaveAndLoadWithdrawalTransaction() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId walletId =
                new WalletId(UUID.randomUUID());

        WalletId unusedWalletId =
                new WalletId(UUID.randomUUID());

        createWalletFixtures(
                walletId,
                unusedWalletId
        );

        Instant createdAt = Instant.parse(
                "2026-09-02T14:00:00Z"
        );

        Transaction transaction =
                Transaction.createWithdrawal(
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("175.00"),
                                Currency.INR
                        ),
                        createdAt,
                        "withdrawal-test"
                );

        transaction.complete();

        transactionRepository.save(transaction);

        Transaction loaded =
                transactionRepository.findById(
                        transactionId
                ).orElseThrow();

        assertThat(loaded.id())
                .isEqualTo(transactionId);

        assertThat(loaded.type())
                .isEqualTo(TransactionType.WITHDRAWAL);

        assertThat(loaded.sourceWalletId())
                .isEqualTo(walletId);

        assertThat(loaded.destinationWalletId())
                .isNull();

        assertThat(loaded.amount().amount())
                .isEqualByComparingTo("175.00");

        assertThat(loaded.currency())
                .isEqualTo(Currency.INR);

        assertThat(loaded.createdAt())
                .isEqualTo(createdAt);

        assertThat(loaded.status())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldPersistWithdrawalWithNullableDestinationWallet() {
        TransactionId transactionId =
                new TransactionId(UUID.randomUUID());

        WalletId walletId =
                new WalletId(UUID.randomUUID());

        WalletId unusedWalletId =
                new WalletId(UUID.randomUUID());

        createWalletFixtures(
                walletId,
                unusedWalletId
        );

        Transaction transaction =
                Transaction.createWithdrawal(
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("125.00"),
                                Currency.INR
                        ),
                        Instant.parse(
                                "2026-09-02T14:30:00Z"
                        ),
                        "withdrawal-persistence-test"
                );

        transaction.complete();

        transactionRepository.save(transaction);

        TransactionEntity entity =
                springDataRepository
                        .findById(transactionId.value())
                        .orElseThrow();

        assertThat(entity.getType())
                .isEqualTo("WITHDRAWAL");

        assertThat(entity.getSourceWalletId())
                .isEqualTo(walletId.value());

        assertThat(entity.getDestinationWalletId())
                .isNull();

        assertThat(entity.getAmount())
                .isEqualByComparingTo("125.00");

        assertThat(entity.getCurrency())
                .isEqualTo(Currency.INR);

        assertThat(entity.getStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    private void createWalletFixtures(
            WalletId sourceWalletId,
            WalletId destinationWalletId
    ) {
        UUID sourceAccountId =
                UUID.randomUUID();

        UUID destinationAccountId =
                UUID.randomUUID();

        Instant createdAt =
                Instant.now();

        accountRepository.save(
                new AccountEntity(
                        sourceAccountId,
                        "alice@example.com",
                        "Alice",
                        "Smith",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        createdAt,
                        createdAt
                )
        );

        accountRepository.save(
                new AccountEntity(
                        destinationAccountId,
                        "bob@example.com",
                        "Bob",
                        "Jones",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        createdAt,
                        createdAt
                )
        );

        walletRepository.save(
                new WalletEntity(
                        sourceWalletId.value(),
                        sourceAccountId,
                        Currency.INR,
                        new BigDecimal("10000.00")
                )
        );

        walletRepository.save(
                new WalletEntity(
                        destinationWalletId.value(),
                        destinationAccountId,
                        Currency.INR,
                        new BigDecimal("10000.00")
                )
        );
    }
}