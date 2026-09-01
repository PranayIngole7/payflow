package com.payflow.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents something meaningful that happened in the domain.
 *
 * Domain events are transport-independent. They do not depend on
 * Kafka, Spring, or any other infrastructure technology.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String eventType();
}