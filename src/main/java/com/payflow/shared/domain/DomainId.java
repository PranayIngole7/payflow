package com.payflow.shared.domain;

import java.util.UUID;

/**
 * Marker interface for domain identifiers.
 */
public interface DomainId {

    UUID value();
}