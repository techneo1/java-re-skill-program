package com.example.helloworld.store;

import java.util.Objects;

/**
 * Custom object used as a HashMap key for Employee lookups.
 *
 * Rules for a correct HashMap key:
 *  1. equals()   — two keys are equal when BOTH id AND email match
 *  2. hashCode() — must be consistent with equals(); same fields must produce same hash
 *  3. Immutability — fields are final so the hash never changes after insertion
 *
 * If either rule is broken:
 *  - Broken equals()   → two logically equal keys resolve to different buckets → duplicates
 *  - Broken hashCode() → a key inserted with hash H cannot be found after mutation → lost entries
 *  - Mutable key       → mutating a field changes the hash → the entry becomes unreachable
 */
public final class EmployeeKey {

    private final int    id;      // numeric part of the composite key
    private final String email;   // string part of the composite key

    public EmployeeKey(int id, String email) {
        if (id <= 0)     throw new IllegalArgumentException("id must be positive");
        Objects.requireNonNull(email, "email must not be null");
        if (email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        this.id    = id;
        this.email = email.strip().toLowerCase();   // normalise once at construction
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getId()    { return id; }
    public String getEmail() { return email; }

    // ── equals ───────────────────────────────────────────────────────────────
    // Two EmployeeKeys are equal only when BOTH id AND email match.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeKey other)) return false;
        return id == other.id
                && email.equals(other.email);
    }

    // ── hashCode ─────────────────────────────────────────────────────────────
    // Must use the SAME fields as equals() — id and email.
    // Objects.hash() combines them into a single int using prime multiplication.
    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "EmployeeKey{id=" + id + ", email='" + email + "'}";
    }
}

