package com.example.helloworld.service;

import com.example.helloworld.domain.ContractEmployee;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.ValidationException;

/**
 * Validates Employee fields against business rules.
 */
public class EmployeeValidationService implements ValidationService {

    @Override
    public void validate(Employee employee) throws ValidationException {
        if (employee.getId() <= 0)
            throw new ValidationException("id", employee.getId(), "must be positive");

        if (employee.getName() == null || employee.getName().isBlank())
            throw new ValidationException("name", employee.getName(), "must not be blank");

        if (employee.getEmail() == null || employee.getEmail().isBlank())
            throw new ValidationException("email", employee.getEmail(), "must not be blank");

        if (!employee.getEmail().contains("@"))
            throw new ValidationException("email", employee.getEmail(), "must contain '@'");

        if (employee.getSalary() < 0)
            throw new ValidationException("salary", employee.getSalary(), "must not be negative");

        if (employee.getStatus() == null)
            throw new ValidationException("status", null, "must not be null");

        if (employee.getStatus() == EmployeeStatus.INACTIVE)
            throw new ValidationException("status", employee.getStatus(),
                    "cannot add or update an INACTIVE employee");

        if (employee.getDepartmentId() <= 0)
            throw new ValidationException("departmentId", employee.getDepartmentId(), "must be positive");

        if (employee instanceof ContractEmployee ce && ce.isExpired())
            throw new ValidationException("contractEndDate", ce.getContractEndDate(),
                    "contract has already expired");
    }
}

