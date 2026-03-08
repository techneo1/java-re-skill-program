package com.example.helloworld.exception;

/**
 * Thrown when a payroll calculation cannot be completed for an employee.
 * Examples: expired contract, inactive employee.
 */
public class PayrollException extends EmployeeException {

    private final int employeeId;

    public PayrollException(int employeeId, String reason) {
        super("Payroll error for employee id " + employeeId + ": " + reason);
        this.employeeId = employeeId;
    }

    public int getEmployeeId() { return employeeId; }
}

