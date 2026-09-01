package com.payflow.wallet.infrastructure.persistence;

import com.payflow.shared.domain.Money;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WalletRepositoryAdapter implements WalletRepository {

    private final SpringDataWalletRepository repository;

    public WalletRepositoryAdapter(
            SpringDataWalletRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<Wallet> findById(WalletId walletId) {
        return repository.findById(walletId.value())
                .map(this::toDomain);
    }

    @Override
    public void save(Wallet wallet) {
        repository.save(toEntity(wallet));
    }

    private Wallet toDomain(WalletEntity entity) {
        return Wallet.reconstitute(
                new WalletId(entity.getId()),
                new com.payflow.account.domain.AccountId(
                        entity.getAccountId()
                ),
                entity.getCurrency(),
                new Money(
                        entity.getBalance(),
                        entity.getCurrency()
                )
        );
    }

    private WalletEntity toEntity(Wallet wallet) {
        return new WalletEntity(
                wallet.id().value(),
                wallet.accountId().value(),
                wallet.currency(),
                wallet.balance().amount()
        );
    }
}