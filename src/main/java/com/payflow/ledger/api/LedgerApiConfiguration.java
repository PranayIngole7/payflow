package com.payflow.ledger.api;

import com.payflow.ledger.application.GetLedgerEntriesByAccountUseCase;
import com.payflow.ledger.application.GetLedgerEntriesByTransactionUseCase;
import com.payflow.ledger.application.LedgerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerApiConfiguration {

    @Bean
    public GetLedgerEntriesByTransactionUseCase
    getLedgerEntriesByTransactionUseCase(
            LedgerRepository ledgerRepository
    ) {
        return new GetLedgerEntriesByTransactionUseCase(
                ledgerRepository
        );
    }

    @Bean
    public GetLedgerEntriesByAccountUseCase
    getLedgerEntriesByAccountUseCase(
            LedgerRepository ledgerRepository
    ) {
        return new GetLedgerEntriesByAccountUseCase(
                ledgerRepository
        );
    }
}
