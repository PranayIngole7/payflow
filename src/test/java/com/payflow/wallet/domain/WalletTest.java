package com.payflow.wallet.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void shouldCreateWalletWithZeroBalance() {
        WalletId walletId = WalletId.generate();
        var accountId = com.payflow.account.domain.AccountId.generate();

        Wallet wallet = Wallet.create(walletId, accountId, Currency.INR);

        assertEquals(walletId, wallet.id());
        assertEquals(accountId, wallet.accountId());
        assertEquals(Currency.INR, wallet.currency());
        assertEquals(BigDecimal.ZERO, wallet.balance().amount());
        assertEquals(Currency.INR, wallet.balance().currency());
    }

    @Test
    void shouldCreditWallet() {
        Wallet wallet = createWallet();

        wallet.credit(new Money(new BigDecimal("500.00"), Currency.INR));

        assertEquals(
                new BigDecimal("500.00"),
                wallet.balance().amount()
        );
    }

    @Test
    void shouldDebitWallet() {
        Wallet wallet = createWallet();

        wallet.credit(new Money(new BigDecimal("500.00"), Currency.INR));
        wallet.debit(new Money(new BigDecimal("200.00"), Currency.INR));

        assertEquals(
                new BigDecimal("300.00"),
                wallet.balance().amount()
        );
    }

    @Test
    void shouldRejectDebitWhenFundsAreInsufficient() {
        Wallet wallet = createWallet();

        wallet.credit(new Money(new BigDecimal("100.00"), Currency.INR));

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.debit(
                        new Money(new BigDecimal("100.01"), Currency.INR)
                )
        );
    }

    @Test
    void shouldRejectWrongCurrency() {
        Wallet wallet = createWallet();

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.credit(
                        new Money(new BigDecimal("100.00"), Currency.USD)
                )
        );
    }

    @Test
    void shouldRejectZeroCredit() {
        Wallet wallet = createWallet();

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.credit(
                        new Money(BigDecimal.ZERO, Currency.INR)
                )
        );
    }

    @Test
    void shouldRejectNegativeCredit() {
        Wallet wallet = createWallet();

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.credit(
                        new Money(new BigDecimal("-10.00"), Currency.INR)
                )
        );
    }

    @Test
    void shouldRejectZeroDebit() {
        Wallet wallet = createWallet();

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.debit(
                        new Money(BigDecimal.ZERO, Currency.INR)
                )
        );
    }

    @Test
    void shouldRejectNegativeDebit() {
        Wallet wallet = createWallet();

        assertThrows(
                IllegalArgumentException.class,
                () -> wallet.debit(
                        new Money(new BigDecimal("-10.00"), Currency.INR)
                )
        );
    }

    @Test
    void shouldRejectNullWalletId() {
        var accountId = com.payflow.account.domain.AccountId.generate();

        assertThrows(
                NullPointerException.class,
                () -> Wallet.create(null, accountId, Currency.INR)
        );
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(
                NullPointerException.class,
                () -> Wallet.create(
                        WalletId.generate(),
                        null,
                        Currency.INR
                )
        );
    }

    @Test
    void shouldRejectNullCurrency() {
        var accountId = com.payflow.account.domain.AccountId.generate();

        assertThrows(
                NullPointerException.class,
                () -> Wallet.create(
                        WalletId.generate(),
                        accountId,
                        null
                )
        );
    }

    private Wallet createWallet() {
        return Wallet.create(
                WalletId.generate(),
                com.payflow.account.domain.AccountId.generate(),
                Currency.INR
        );
    }
}
