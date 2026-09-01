package com.payflow.transaction.application;

import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryId;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.domain.Wallet;

import java.time.Instant;
import java.util.Objects;

/**

 * Application use case responsible for coordinating a wallet-to-wallet
 * transfer.
 *
 * <p>The individual aggregates remain responsible for their own business
 * rules. This class coordinates the operation across aggregates.</p>
 *
 * <p>The complete transfer is executed through {@link TransactionRunner}
 * so that the eventual infrastructure implementation can provide an
 * atomic database transaction.</p>
 */
public final class TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRunner transactionRunner;

    public TransferMoneyUseCase(
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

    /**

     * Executes a wallet-to-wallet transfer inside the application
     * transaction boundary.
     *
     * @param transactionId transaction to process
     */
    public void execute(TransactionId transactionId) {
        Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );

        transactionRunner.execute(
                () -> executeTransfer(transactionId)
        );
    }

    /**

     * Performs the actual transfer operation.
     *
     * <p>This method is deliberately kept separate from {@link #execute}
     * so that the transaction boundary remains explicit at the application
     * entry point.</p>
     */
    private void executeTransfer(TransactionId transactionId) {
        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "transaction not found: " + transactionId
                ));

        Wallet sourceWallet = walletRepository
                .findById(transaction.sourceWalletId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "source wallet not found: "
                                + transaction.sourceWalletId()
                ));

        Wallet destinationWallet = walletRepository
                .findById(transaction.destinationWalletId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "destination wallet not found: "
                                + transaction.destinationWalletId()
                ));

        sourceWallet.debit(transaction.amount());
        destinationWallet.credit(transaction.amount());

        Ledger ledger = ledgerRepository
                .findByTransactionId(transactionId)
                .orElseGet(Ledger::create);

        Instant now = Instant.now();

        ledger.add(
                LedgerEntry.create(
                        LedgerEntryId.generate(),
                        transactionId,
                        sourceWallet.id(),
                        transaction.amount(),
                        LedgerEntryType.DEBIT,
                        now
                )
        );

        ledger.add(
                LedgerEntry.create(
                        LedgerEntryId.generate(),
                        transactionId,
                        destinationWallet.id(),
                        transaction.amount(),
                        LedgerEntryType.CREDIT,
                        now
                )
        );

        if (!ledger.isBalanced()) {
            throw new IllegalStateException(
                    "ledger must be balanced before completing transaction"
            );
        }

        transaction.complete();

        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);
        ledgerRepository.save(ledger);
        transactionRepository.save(transaction);
    }
}
