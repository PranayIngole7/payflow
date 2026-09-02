package com.payflow.transaction.api;

import com.payflow.transaction.domain.TransactionId;

import java.util.UUID;

/**
 * HTTP response returned after a transfer has been initiated.
 */
public record InitiateTransferResponse(
        UUID transactionId
) {

    public static InitiateTransferResponse from(
            TransactionId transactionId
    ) {
        return new InitiateTransferResponse(
                transactionId.value()
        );
    }
}