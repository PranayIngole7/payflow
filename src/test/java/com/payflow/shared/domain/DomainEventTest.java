package com.payflow.shared.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    @Test
    void shouldExposeEventMetadata() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        DomainEvent event = new TestDomainEvent(
                eventId,
                occurredAt,
                "test.event"
        );

        assertEquals(eventId, event.eventId());
        assertEquals(occurredAt, event.occurredAt());
        assertEquals("test.event", event.eventType());
    }

    private record TestDomainEvent(
            UUID eventId,
            Instant occurredAt,
            String eventType
    ) implements DomainEvent {
    }
}