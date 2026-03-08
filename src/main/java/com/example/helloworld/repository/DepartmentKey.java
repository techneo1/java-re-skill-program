package com.example.helloworld.repository;

/**
 * Immutable HashMap key for department-based index lookups.
 * Record auto-generates equals(), hashCode(), and toString().
 */
public record DepartmentKey(int id) {

    public DepartmentKey {
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
    }
}
