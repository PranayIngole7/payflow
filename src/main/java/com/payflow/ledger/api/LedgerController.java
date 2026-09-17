package com.payflow.ledger.api;

import com.payflow.ledger.application.GetLedgerEntriesByAccountUseCase;
import com.payflow.ledger.application.GetLedgerEntriesByTransactionUseCase;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.transaction.domain.TransactionId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final GetLedgerEntriesByTransactionUseCase
            getLedgerEntriesByTransactionUseCase;

    private final GetLedgerEntriesByAccountUseCase
            getLedgerEntriesByAccountUseCase;

    public LedgerController(
            GetLedgerEntriesByTransactionUseCase
                    getLedgerEntriesByTransactionUseCase,
            GetLedgerEntriesByAccountUseCase
                    getLedgerEntriesByAccountUseCase
    ) {
        this.getLedgerEntriesByTransactionUseCase =
                getLedgerEntriesByTransactionUseCase;
        this.getLedgerEntriesByAccountUseCase =
                getLedgerEntriesByAccountUseCase;
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<List<LedgerEntryResponse>>
    getByTransaction(
            @PathVariable UUID transactionId
    ) {
        List<LedgerEntryResponse> response =
                getLedgerEntriesByTransactionUseCase
                        .execute(new TransactionId(transactionId))
                        .stream()
                        .map(LedgerEntryResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<LedgerEntryResponse>>
    getByAccount(
            @PathVariable UUID accountId
    ) {
        List<LedgerEntryResponse> response =
                getLedgerEntriesByAccountUseCase
                        .execute(new LedgerAccountId(accountId))
                        .stream()
                        .map(LedgerEntryResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
}
