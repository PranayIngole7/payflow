package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreatePaymentUseCaseTest {

    private PaymentRepository paymentRepository;
    private IdempotencyKeyRepository idempotencyKeyRepository;
    private PaymentRequestHasher paymentRequestHasher;

    private CreatePaymentUseCase useCase;

    private AccountId accountId;
    private WalletId sourceWalletId;
    private WalletId destinationWalletId;
    private Money money;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        idempotencyKeyRepository = mock(IdempotencyKeyRepository.class);
        paymentRequestHasher = mock(PaymentRequestHasher.class);

        useCase = new CreatePaymentUseCase(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );

        accountId = new AccountId(UUID.randomUUID());
        sourceWalletId = new WalletId(UUID.randomUUID());
        destinationWalletId = new WalletId(UUID.randomUUID());

        money = new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );
    }

    @Test
    void shouldCreatePayment() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-001",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001");

        Payment result =
                useCase.execute(command, "idem-001");

        assertThat(result).isNotNull();
        assertThat(result.reference()).isEqualTo("PAY-001");
        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(result.sourceWalletId())
                .isEqualTo(sourceWalletId);
        assertThat(result.destinationWalletId())
                .isEqualTo(destinationWalletId);
        assertThat(result.amount()).isEqualTo(money);
        assertThat(result.type())
                .isEqualTo(TransactionType.TRANSFER);

        verify(paymentRepository).save(result);

        verify(idempotencyKeyRepository).save(
                eq(accountId),
                eq("idem-001"),
                eq("hash-001"),
                eq(result.id().value()),
                eq(result.createdAt()),
                any()
        );
    }

    @Test
    void shouldReturnExistingPaymentForSameIdempotencyKeyAndRequest() {
        UUID existingPaymentId = UUID.randomUUID();

        IdempotencyKeyRepository.IdempotencyRecord record =
                new IdempotencyKeyRepository.IdempotencyRecord(
                        accountId,
                        "idem-002",
                        "hash-002",
                        existingPaymentId,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        Payment existingPayment = Payment.create(
                new PaymentId(existingPaymentId),
                "PAY-002",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                Instant.now(),
                TransactionType.TRANSFER
        );

        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-002",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "idem-002"
        )).thenReturn(Optional.of(record));

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-002");

        when(paymentRepository.findById(
                new PaymentId(existingPaymentId)
        )).thenReturn(Optional.of(existingPayment));

        Payment result =
                useCase.execute(command, "idem-002");

        assertThat(result).isSameAs(existingPayment);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(idempotencyKeyRepository, never())
                .save(
                        any(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRejectSameIdempotencyKeyForDifferentRequest() {
        IdempotencyKeyRepository.IdempotencyRecord record =
                new IdempotencyKeyRepository.IdempotencyRecord(
                        accountId,
                        "idem-003",
                        "existing-hash",
                        UUID.randomUUID(),
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-003",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "idem-003"
        )).thenReturn(Optional.of(record));

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("different-hash");

        assertThatThrownBy(() ->
                useCase.execute(command, "idem-003")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "idempotency key has already been used"
                );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-004",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "   ")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "idempotency key must not be blank"
                );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldRejectNullIdempotencyKey() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-005",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "idempotency key must not be null"
                );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThan100Characters() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-006",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        String longKey = "a".repeat(101);

        assertThatThrownBy(() ->
                useCase.execute(command, longKey)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "idempotency key must not exceed 100 characters"
                );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() ->
                useCase.execute(null, "idem-007")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "create payment command must not be null"
                );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldNormalizeIdempotencyKeyBeforeLookup() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-008",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-008");

        useCase.execute(command, "  idem-008  ");

        verify(idempotencyKeyRepository)
                .findByAccountIdAndKey(
                        accountId,
                        "idem-008"
                );

        verify(idempotencyKeyRepository)
                .save(
                        eq(accountId),
                        eq("idem-008"),
                        eq("hash-008"),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRejectTransferWithoutSourceWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-001",
                accountId,
                null,
                destinationWalletId,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-001")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "source wallet id must not be null"
                );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectTransferWithoutDestinationWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-002",
                accountId,
                sourceWalletId,
                null,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-002")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "destination wallet id must not be null"
                );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectTransferWithSameSourceAndDestinationWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-003",
                accountId,
                sourceWalletId,
                sourceWalletId,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-003")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectDepositWithSourceWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-004",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.DEPOSIT
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-004")
        )
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectDepositWithoutDestinationWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-005",
                accountId,
                null,
                null,
                money,
                TransactionType.DEPOSIT
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-005")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "deposit destination wallet must not be null"
                );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectWithdrawalWithoutSourceWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-006",
                accountId,
                null,
                null,
                money,
                TransactionType.WITHDRAWAL
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-006")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(
                        "withdrawal source wallet must not be null"
                );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldRejectWithdrawalWithDestinationWallet() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-007",
                accountId,
                sourceWalletId,
                destinationWalletId,
                money,
                TransactionType.WITHDRAWAL
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-007")
        )
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(paymentRepository);
    }
    
    @Test
    void shouldNotSaveIdempotencyRecordWhenPaymentValidationFails() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "PAY-SEM-008",
                accountId,
                sourceWalletId,
                sourceWalletId,
                money,
                TransactionType.TRANSFER
        );

        assertThatThrownBy(() ->
                useCase.execute(command, "semantic-008")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verifyNoInteractions(paymentRepository);

        verify(idempotencyKeyRepository)
                .findByAccountIdAndKey(
                        accountId,
                        "semantic-008"
                );

        verify(idempotencyKeyRepository, never())
                .save(
                        any(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any()
                );

        verifyNoInteractions(paymentRequestHasher);
    }
    
}
