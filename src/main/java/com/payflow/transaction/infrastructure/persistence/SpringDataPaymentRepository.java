package com.payflow.transaction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataPaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {
}