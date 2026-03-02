package com.example.helloworld.exception;

/**
 * Thrown when an Employee lookup by id or email finds no match.
 * Carries the id (or email) that was searched for.
 */
public class EmployeeNotFoundException extends EmployeeException {

    private final String searchKey;

    public EmployeeNotFoundException(int id) {
        super("No employee found with id: " + id);
        this.searchKey = String.valueOf(id);
    }

    public EmployeeNotFoundException(String email) {
        super("No employee found with email: " + email);
        this.searchKey = email;
    }

    public String getSearchKey() {
        return searchKey;
    }
}

