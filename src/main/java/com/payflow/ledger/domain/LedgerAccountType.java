package com.payflow.ledger.domain;

/**
 * Classification of an accounting ledger account.
 */
public enum LedgerAccountType {

    /**
     * Accounting account representing a PayFlow wallet.
     */
    WALLET,

    /**
     * Counterparty account representing funds entering PayFlow.
     */
    EXTERNAL_FUNDING,

    /**
     * Counterparty account representing funds leaving PayFlow.
     */
    EXTERNAL_SETTLEMENT
}

