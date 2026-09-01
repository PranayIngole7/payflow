package com.payflow.ledger.domain;

/**
 * Accounting direction of a ledger entry.
 *
 * <p>A financial operation is represented using debit and credit entries
 * rather than modifying historical records.</p>
 */
public enum LedgerEntryType {

    /**
     * Records value leaving an account or wallet.
     */
    DEBIT,

    /**
     * Records value entering an account or wallet.
     */
    CREDIT
}