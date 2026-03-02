package com.example.helloworld.model;

import java.util.Objects;

/**
 * Represents a Department in the organization.
 * Used as a DTO — implemented as a Java Record for immutability.
 * Records automatically provide equals(), hashCode(), and toString().
 */
public record Department(
        int id,
        String name,
        String location
) {
    // Compact canonical constructor for validation
    public Department {
        Objects.requireNonNull(name, "Department name must not be null");
        Objects.requireNonNull(location, "Department location must not be null");
        if (id <= 0) throw new IllegalArgumentException("Department id must be positive");
        if (name.isBlank()) throw new IllegalArgumentException("Department name must not be blank");
    }
}

