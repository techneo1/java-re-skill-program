package com.example.helloworld.store;

import java.util.Objects;

/**
 * Record-based HashMap key for Department lookups.
 *
 * Java Records are ideal as HashMap keys because:
 *  - All fields are implicitly final (immutable by design)
 *  - equals() and hashCode() are auto-generated using ALL record components
 *  - No boilerplate needed — the compiler guarantees correctness
 *
 * Contrast with EmployeeKey (manual class) to see both approaches side by side.
 */
public record DepartmentKey(int id, String name) {

    // Compact canonical constructor for validation
    public DepartmentKey {
        if (id <= 0)    throw new IllegalArgumentException("id must be positive");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        name = name.strip().toLowerCase();   // normalise for consistent key matching
    }
}

