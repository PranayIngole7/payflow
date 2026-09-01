package com.payflow.shared.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SpringTransactionRunnerTest {

    @Test
    void shouldImplementTransactionRunner() {
        SpringTransactionRunner runner =
                new SpringTransactionRunner();

        assertInstanceOf(
                com.payflow.shared.application.TransactionRunner.class,
                runner
        );
    }

    @Test
    void shouldExecuteWork() {
        SpringTransactionRunner runner =
                new SpringTransactionRunner();

        AtomicBoolean executed = new AtomicBoolean(false);

        runner.execute(() -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void shouldRejectNullWork() {
        SpringTransactionRunner runner =
                new SpringTransactionRunner();

        assertThrows(
                NullPointerException.class,
                () -> runner.execute(null)
        );
    }

    @Test
    void executeShouldBeTransactional() throws NoSuchMethodException {
        Transactional annotation =
                SpringTransactionRunner.class
                        .getMethod(
                                "execute",
                                Runnable.class
                        )
                        .getAnnotation(Transactional.class);

        assertNotNull(annotation);
    }
}
