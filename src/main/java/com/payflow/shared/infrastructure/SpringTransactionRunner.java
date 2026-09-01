package com.payflow.shared.infrastructure;

import com.payflow.shared.application.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Spring-backed implementation of the application transaction boundary.
 *
 * <p>The application layer depends only on {@link TransactionRunner}.
 * Spring transaction management is kept in infrastructure.</p>
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    @Override
    @Transactional
    public void execute(Runnable work) {
        Objects.requireNonNull(
                work,
                "transaction work must not be null"
        );

        work.run();
    }
}
