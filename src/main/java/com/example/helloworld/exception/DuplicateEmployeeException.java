package com.example.helloworld.exception;

/**
 * Thrown when attempting to add an Employee whose id already exists in the store.
 */
public class DuplicateEmployeeException extends EmployeeException {

    private final int duplicateId;

    public DuplicateEmployeeException(int id) {
        super("Employee with id " + id + " already exists");
        this.duplicateId = id;
    }

    public int getDuplicateId() {
        return duplicateId;
    }
}

