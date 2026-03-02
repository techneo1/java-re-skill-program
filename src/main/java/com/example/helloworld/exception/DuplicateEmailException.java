package com.example.helloworld.exception;

/**
 * Thrown when attempting to add or update an Employee with an email
 * that is already registered to another employee in the store.
 */
public class DuplicateEmailException extends EmployeeException {

    private final String duplicateEmail;

    public DuplicateEmailException(String email) {
        super("Email '" + email + "' is already taken by another employee");
        this.duplicateEmail = email;
    }

    public String getDuplicateEmail() {
        return duplicateEmail;
    }
}

