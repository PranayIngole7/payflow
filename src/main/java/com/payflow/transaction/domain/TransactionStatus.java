package com.payflow.transaction.domain;

/**
 * Lifecycle state of a financial transaction.
 *
 * <p>A transaction starts in PENDING state and moves to a terminal
 * state once processing succeeds or fails.</p>
 */
public enum TransactionStatus {

    /**
     * Transaction has been created but financial processing is not complete.
     */
    PENDING,

    /**
     * Transaction completed successfully.
     */
    COMPLETED,

    /**
     * Transaction could not be completed.
     */
    FAILED
}
