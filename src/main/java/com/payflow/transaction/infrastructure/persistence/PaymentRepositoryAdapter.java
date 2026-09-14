package com.payflow.transaction.infrastructure.persistence;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.PaymentRepository;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRepositoryAdapter
        implements PaymentRepository {

    private final SpringDataPaymentRepository paymentRepository;

    public PaymentRepositoryAdapter(
            SpringDataPaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void save(Payment payment) {

        PaymentEntity entity = new PaymentEntity(
                payment.id().value(),
                payment.reference(),
                payment.accountId().value(),
                payment.sourceWalletId() == null
                        ? null
                        : payment.sourceWalletId().value(),
                payment.destinationWalletId() == null
                        ? null
                        : payment.destinationWalletId().value(),
                payment.type().name(),
                payment.amount().amount(),
                payment.currency(),
                toEntityStatus(payment.status()),
                payment.createdAt(),
                payment.updatedAt()
        );

        paymentRepository.save(entity);
    }

    @Override
    public Optional<Payment> findById(
            PaymentId paymentId
    ) {
        return paymentRepository.findById(paymentId.value())
                .map(this::toDomain);
    }

    private PaymentEntity.PaymentStatusValue toEntityStatus(
            PaymentStatus status
    ) {
        return switch (status) {
            case PENDING ->
                    PaymentEntity.PaymentStatusValue.PENDING;

            case COMPLETED ->
                    PaymentEntity.PaymentStatusValue.COMPLETED;

            case FAILED ->
                    PaymentEntity.PaymentStatusValue.FAILED;

            case CANCELLED ->
                    PaymentEntity.PaymentStatusValue.CANCELLED;
        };
    }

    private Payment toDomain(
            PaymentEntity entity
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

        return Payment.reconstitute(
                new PaymentId(entity.getId()),
                entity.getReference(),
                new AccountId(entity.getUserId()),
                sourceWalletId,
                destinationWalletId,
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                TransactionType.valueOf(entity.getType()),
                PaymentStatus.valueOf(
                        entity.getStatus().name()
                )
        );
    }
}