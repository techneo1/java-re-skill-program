package com.example.helloworld.exception;

/**
 * Base checked exception for all employee store operations.
 * Extend this class for specific error scenarios so callers can catch
 * either the broad base type or a specific subtype.
 */
public class EmployeeException extends Exception {

    public EmployeeException(String message) {
        super(message);
    }

    public EmployeeException(String message, Throwable cause) {
        super(message, cause);
    }
}

