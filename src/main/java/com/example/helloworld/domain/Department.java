package com.example.helloworld.domain;

import java.util.Objects;

public record Department(int id, String name, String location) {
    public Department {
        Objects.requireNonNull(name, "Department name must not be null");
        Objects.requireNonNull(location, "Department location must not be null");
        if (id <= 0) throw new IllegalArgumentException("Department id must be positive");
        if (name.isBlank()) throw new IllegalArgumentException("Department name must not be blank");
    }
}

