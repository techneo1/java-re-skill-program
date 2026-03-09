package com.example.helloworld.exception;

/**
 * Thrown when an employee's departmentId does not correspond to any known
 * department in the system.
 *
 * Sits in the {@link EmployeeException} hierarchy so callers can catch either
 * the specific subtype or the broad base class.
 */
public class DepartmentNotFoundException extends EmployeeException {

    private final int departmentId;

    public DepartmentNotFoundException(int departmentId) {
        super("Department with id " + departmentId + " does not exist");
        this.departmentId = departmentId;
    }

    public int getDepartmentId() {
        return departmentId;
    }
}

