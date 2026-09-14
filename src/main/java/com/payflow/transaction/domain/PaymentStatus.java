package com.payflow.transaction.domain;

/**
 * Lifecycle state of a payment.
 *
 * <p>A payment starts in PENDING state and moves to one
 * terminal state once processing succeeds, fails, or the
 * payment is cancelled.</p>
 */
public enum PaymentStatus {

    /**
     * Payment has been created but processing is not complete.
     */
    PENDING,

    /**
     * Payment completed successfully.
     */
    COMPLETED,

    /**
     * Payment could not be completed.
     */
    FAILED,

    /**
     * Payment was cancelled before completion.
     */
    CANCELLED
}