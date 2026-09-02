package com.payflow.ledger.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate representing the ledger of financial entries.
 *
 * <p>Ledger entries are append-only financial history. Entries may be added,
 * but existing entries cannot be modified or removed.</p>
 *
 * <p>A ledger belongs to exactly one transaction.</p>
 */
public final class Ledger {

    private TransactionId transactionId;

    private final List<LedgerEntry> entries;

    private Ledger() {
        this.entries = new ArrayList<>();
    }

    /**
     * Creates an empty ledger.
     */
    public static Ledger create() {
        return new Ledger();
    }

    /**
     * Reconstructs an existing ledger from persistent state.
     *
     * <p>The supplied entries are restored through the aggregate's
     * normal invariant checks. Existing entries remain immutable and
     * insertion order is preserved.</p>
     *
     * @param transactionId transaction represented by this ledger
     * @param entries persisted ledger entries
     * @return reconstructed ledger
     */
    public static Ledger reconstitute(
            TransactionId transactionId,
            List<LedgerEntry> entries
    ) {
        Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );
        Objects.requireNonNull(
                entries,
                "ledger entries must not be null"
        );

        Ledger ledger = new Ledger();
        ledger.transactionId = transactionId;

        for (LedgerEntry entry : entries) {
            Objects.requireNonNull(
                    entry,
                    "ledger entries must not contain null"
            );

            ledger.add(entry);
        }

        return ledger;
    }

    /**
     * Creates a balanced ledger for a transaction.
     *
     * <p>The source wallet receives a debit entry and the destination wallet
     * receives a credit entry for the same amount.</p>
     *
     * @param transaction transaction to represent in the ledger
     * @return a balanced ledger containing exactly two entries
     */
    public static Ledger from(Transaction transaction) {
        Objects.requireNonNull(
                transaction,
                "transaction must not be null"
        );

        Ledger ledger = new Ledger();

        Instant now = Instant.now();

        LedgerEntry debit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transaction.id(),
                transaction.sourceWalletId(),
                transaction.amount(),
                LedgerEntryType.DEBIT,
                now
        );

        LedgerEntry credit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transaction.id(),
                transaction.destinationWalletId(),
                transaction.amount(),
                LedgerEntryType.CREDIT,
                now
        );

        ledger.add(debit);
        ledger.add(credit);

        return ledger;
    }

    public Optional<TransactionId> transactionId() {
        return Optional.ofNullable(transactionId);
    }

    /**
     * Adds a ledger entry to the ledger.
     */
    public void add(LedgerEntry entry) {
        Objects.requireNonNull(
                entry,
                "ledger entry must not be null"
        );

        if (transactionId == null) {
            transactionId = entry.transactionId();
        } else if (!transactionId.equals(entry.transactionId())) {
            throw new IllegalArgumentException(
                    "ledger entries must belong to the same transaction"
            );
        }

        entries.add(entry);
    }

    /**
     * Returns ledger entries in insertion order.
     */
    public List<LedgerEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Determines whether debit and credit totals are equal.
     *
     * <p>An empty ledger is not balanced. Both debit and credit sides must
     * exist, and all entries must use the same currency.</p>
     */
    public boolean isBalanced() {
        if (entries.isEmpty()) {
            return false;
        }

        Currency currency = entries.getFirst().currency();

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        boolean hasDebit = false;
        boolean hasCredit = false;

        for (LedgerEntry entry : entries) {
            if (entry.currency() != currency) {
                return false;
            }

            if (entry.type() == LedgerEntryType.DEBIT) {
                debitTotal = debitTotal.add(entry.amount().amount());
                hasDebit = true;
            } else {
                creditTotal = creditTotal.add(entry.amount().amount());
                hasCredit = true;
            }
        }

        return hasDebit
                && hasCredit
                && debitTotal.compareTo(creditTotal) == 0;
    }
}