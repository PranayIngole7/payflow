package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.transaction.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetPaymentHistoryUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentHistoryUseCase(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<Payment> execute(AccountId accountId) {
        return paymentRepository.findByAccountId(accountId);
    }
}
