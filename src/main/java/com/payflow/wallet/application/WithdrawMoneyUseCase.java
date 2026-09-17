package com.payflow.wallet.application;

import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryId;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class WithdrawMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRunner transactionRunner;

    public WithdrawMoneyUseCase(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            TransactionRunner transactionRunner
    ) {
        this.walletRepository = Objects.requireNonNull(
                walletRepository,
                "wallet repository must not be null"
        );

        this.transactionRepository = Objects.requireNonNull(
                transactionRepository,
                "transaction repository must not be null"
        );

        this.ledgerRepository = Objects.requireNonNull(
                ledgerRepository,
                "ledger repository must not be null"
        );

        this.transactionRunner = Objects.requireNonNull(
                transactionRunner,
                "transaction runner must not be null"
        );
    }

    public Wallet execute(
            WalletId walletId,
            BigDecimal amount,
            Currency currency
    ) {
        Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );

        Objects.requireNonNull(
                amount,
                "withdrawal amount must not be null"
        );

        Objects.requireNonNull(
                currency,
                "withdrawal currency must not be null"
        );

        Money money = new Money(amount, currency);

        final Wallet[] result = new Wallet[1];

        transactionRunner.execute(() -> {

            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "wallet not found: " + walletId.value()
                    ));

            wallet.debit(money);

            Transaction transaction = Transaction.createWithdrawal(
                    TransactionId.generate(),
                    walletId,
                    money,
                    Instant.now(),
                    "withdrawal-" + walletId.value() + "-" + System.nanoTime()
            );

            Ledger ledger = ledgerRepository
                    .findByTransactionId(transaction.id())
                    .orElseGet(Ledger::create);

            Instant now = Instant.now();

            ledger.add(
                    LedgerEntry.create(
                            LedgerEntryId.generate(),
                            transaction.id(),
                            LedgerAccountId.forWallet(wallet.id()),
                            LedgerAccountType.WALLET,
                            money,
                            LedgerEntryType.DEBIT,
                            now
                    )
            );

            ledger.add(
                    LedgerEntry.create(
                            LedgerEntryId.generate(),
                            transaction.id(),
                            LedgerAccountId.externalSettlement(),
                            LedgerAccountType.EXTERNAL_SETTLEMENT,
                            money,
                            LedgerEntryType.CREDIT,
                            now
                    )
            );

            if (!ledger.isBalanced()) {
                throw new IllegalStateException(
                        "ledger must be balanced before completing withdrawal"
                );
            }

            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            ledgerRepository.save(ledger);

            result[0] = wallet;
        });

        return result[0];
    }
}
