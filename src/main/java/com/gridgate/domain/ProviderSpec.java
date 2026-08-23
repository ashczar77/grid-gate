package com.gridgate.domain;

import java.util.Objects;

/**
 * A provider to call as part of a run, in cascade order.
 */
public record ProviderSpec(String id, String name, String phoneE164) {

    public ProviderSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(phoneE164, "phoneE164");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (phoneE164.isBlank()) {
            throw new IllegalArgumentException("phoneE164 must not be blank");
        }
    }
}
