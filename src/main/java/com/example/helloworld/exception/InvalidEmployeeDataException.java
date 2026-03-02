package com.example.helloworld.exception;

/**
 * Thrown when an Employee field value fails a business validation rule.
 * Examples: negative salary, invalid salary range (min > max), blank name.
 */
public class InvalidEmployeeDataException extends EmployeeException {

    private final String fieldName;
    private final Object rejectedValue;

    public InvalidEmployeeDataException(String fieldName, Object rejectedValue, String reason) {
        super("Invalid value for field '" + fieldName + "' = [" + rejectedValue + "]: " + reason);
        this.fieldName     = fieldName;
        this.rejectedValue = rejectedValue;
    }

    public String getFieldName()      { return fieldName; }
    public Object getRejectedValue()  { return rejectedValue; }
}

