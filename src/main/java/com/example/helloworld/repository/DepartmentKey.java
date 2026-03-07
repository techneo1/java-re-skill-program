package com.example.helloworld.repository;

import java.util.Objects;

public record DepartmentKey(int id, String name) {

    public DepartmentKey {
        if (id <= 0)        throw new IllegalArgumentException("id must be positive");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        name = name.strip().toLowerCase();
    }
}

