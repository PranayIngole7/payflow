package com.payflow.transaction.infrastructure.persistence;

import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepositoryAdapter
        implements TransactionRepository {

    private final SpringDataTransactionRepository transactionRepository;
    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataIdempotencyKeyRepository idempotencyKeyRepository;
    private final SpringDataWalletRepository walletRepository;

    public TransactionRepositoryAdapter(
            SpringDataTransactionRepository transactionRepository,
            SpringDataPaymentRepository paymentRepository,
            SpringDataIdempotencyKeyRepository idempotencyKeyRepository,
            SpringDataWalletRepository walletRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public Optional<Transaction> findById(
            TransactionId transactionId
    ) {
        return transactionRepository.findById(transactionId.value())
                .map(this::toDomainWithIdempotencyKey);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey
    ) {
        return idempotencyKeyRepository.findByKey(idempotencyKey)
                .flatMap(key ->
                        transactionRepository.findById(
                                key.getPaymentId()
                        )
                )
                .map(entity -> {
                    entity.setIdempotencyKey(idempotencyKey);
                    return toDomain(entity);
                });
    }

    @Override
    @Transactional
    public void save(Transaction transaction) {

        UUID transactionId = transaction.id().value();

        UUID walletId = switch (transaction.type()) {
            case TRANSFER, WITHDRAWAL ->
                    transaction.sourceWalletId().value();

            case DEPOSIT ->
                    transaction.destinationWalletId().value();
        };

        UUID userId = walletRepository
                .findById(walletId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "wallet not found: " + walletId
                        )
                )
                .getAccountId();

        String reference = transactionId.toString();
        Instant now = Instant.now();

        PaymentEntity.PaymentStatusValue paymentStatus =
                switch (transaction.status()) {
                    case PENDING ->
                            PaymentEntity.PaymentStatusValue.PENDING;

                    case COMPLETED ->
                            PaymentEntity.PaymentStatusValue.COMPLETED;

                    case FAILED ->
                            PaymentEntity.PaymentStatusValue.FAILED;
                };

        PaymentEntity payment = new PaymentEntity(
                transactionId,
                reference,
                userId,
                transaction.sourceWalletId() == null
                        ? null
                        : transaction.sourceWalletId().value(),
                transaction.destinationWalletId() == null
                        ? null
                        : transaction.destinationWalletId().value(),
                transaction.type().name(),
                transaction.amount().amount(),
                transaction.currency(),
                paymentStatus,
                transaction.createdAt(),
                now
        );

        paymentRepository.save(payment);

        Instant completedAt =
                transaction.status()
                        == com.payflow.transaction.domain.TransactionStatus.COMPLETED
                        ? now
                        : null;

        TransactionEntity entity = new TransactionEntity(
                transactionId,
                reference,
                transactionId,
                transaction.sourceWalletId() == null
                        ? null
                        : transaction.sourceWalletId().value(),
                transaction.destinationWalletId() == null
                        ? null
                        : transaction.destinationWalletId().value(),
                transaction.type().name(),
                transaction.amount().amount(),
                transaction.currency(),
                transaction.createdAt(),
                transaction.status(),
                completedAt
        );

        entity.setIdempotencyKey(
                transaction.idempotencyKey()
        );

        transactionRepository.save(entity);

        String requestHash = hashRequest(transaction);

        IdempotencyKeyEntity idempotencyKey =
                new IdempotencyKeyEntity(
                        UUID.nameUUIDFromBytes(
                                (userId + ":" + transaction.idempotencyKey())
                                        .getBytes(StandardCharsets.UTF_8)
                        ),
                        userId,
                        transaction.idempotencyKey(),
                        requestHash,
                        transactionId,
                        transaction.createdAt(),
                        transaction.createdAt()
                                .plus(1, ChronoUnit.DAYS)
                );

        idempotencyKeyRepository.save(idempotencyKey);
    }

    private Transaction toDomainWithIdempotencyKey(
            TransactionEntity entity
    ) {
        idempotencyKeyRepository
                .findByPaymentId(entity.getPaymentId())
                .ifPresent(key ->
                        entity.setIdempotencyKey(key.getKey())
                );

        return toDomain(entity);
    }

    private Transaction toDomain(
            TransactionEntity entity
    ) {
        WalletId sourceWalletId =
                entity.getSourceWalletId() == null
                        ? null
                        : new WalletId(
                                entity.getSourceWalletId()
                        );

        WalletId destinationWalletId =
                entity.getDestinationWalletId() == null
                        ? null
                        : new WalletId(
                                entity.getDestinationWalletId()
                        );

        return Transaction.reconstitute(
                new TransactionId(entity.getId()),
                sourceWalletId,
                destinationWalletId,
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                entity.getCreatedAt(),
                entity.getIdempotencyKey(),
                TransactionType.valueOf(entity.getType()),
                entity.getStatus()
        );
    }

    private String hashRequest(
            Transaction transaction
    ) {
        String sourceWallet =
                transaction.sourceWalletId() == null
                        ? ""
                        : transaction.sourceWalletId()
                                .value()
                                .toString();

        String destinationWallet =
                transaction.destinationWalletId() == null
                        ? ""
                        : transaction.destinationWalletId()
                                .value()
                                .toString();

        String canonical =
                transaction.type()
                        + "|"
                        + sourceWallet
                        + "|"
                        + destinationWallet
                        + "|"
                        + transaction.amount().amount()
                        + "|"
                        + transaction.currency();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}