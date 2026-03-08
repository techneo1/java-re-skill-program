package com.example.helloworld.exception;

/**
 * Thrown when an employee fails a business validation rule.
 * Examples: inactive employee on add, blank name, negative salary.
 */
public class ValidationException extends EmployeeException {

    private final String fieldName;
    private final Object rejectedValue;

    public ValidationException(String fieldName, Object rejectedValue, String reason) {
        super("Validation failed for field '" + fieldName + "' = [" + rejectedValue + "]: " + reason);
        this.fieldName     = fieldName;
        this.rejectedValue = rejectedValue;
    }

    public String getFieldName()     { return fieldName; }
    public Object getRejectedValue() { return rejectedValue; }
}

