package com.payflow.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainFoundationTest {

    @Test
    void shouldRepresentDomainId() {
        UUID value = UUID.randomUUID();

        DomainId id = new TestDomainId(value);

        assertEquals(value, id.value());
    }

    @Test
    void shouldRepresentDomainException() {
        DomainException exception =
                new DomainException("business rule violated");

        assertEquals(
                "business rule violated",
                exception.getMessage()
        );
    }

    private record TestDomainId(UUID value) implements DomainId {
    }
}