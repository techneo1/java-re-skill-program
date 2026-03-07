package com.example.helloworld.repository;

import java.util.Objects;

public final class EmployeeKey {

    private final int    id;
    private final String email;

    public EmployeeKey(int id, String email) {
        if (id <= 0)         throw new IllegalArgumentException("id must be positive");
        Objects.requireNonNull(email, "email must not be null");
        if (email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        this.id    = id;
        this.email = email.strip().toLowerCase();
    }

    public int    getId()    { return id; }
    public String getEmail() { return email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeKey other)) return false;
        return id == other.id && email.equals(other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "EmployeeKey{id=" + id + ", email='" + email + "'}";
    }
}

