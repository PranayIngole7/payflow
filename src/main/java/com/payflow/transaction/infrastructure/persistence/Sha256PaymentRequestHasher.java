package com.payflow.transaction.infrastructure.persistence;

import com.payflow.transaction.application.PaymentRequestHasher;
import com.payflow.transaction.domain.Payment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class Sha256PaymentRequestHasher
        implements PaymentRequestHasher {

    @Override
    public String hash(Payment payment) {

        String sourceWallet =
                payment.sourceWalletId() == null
                        ? ""
                        : payment.sourceWalletId().value().toString();

        String destinationWallet =
                payment.destinationWalletId() == null
                        ? ""
                        : payment.destinationWalletId().value().toString();

        String canonical =
                payment.type()
                        + "|"
                        + sourceWallet
                        + "|"
                        + destinationWallet
                        + "|"
                        + payment.amount().amount()
                        + "|"
                        + payment.currency();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}
