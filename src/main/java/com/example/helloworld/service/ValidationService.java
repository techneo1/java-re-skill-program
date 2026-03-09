package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.exception.DepartmentNotFoundException;
import com.example.helloworld.exception.DuplicateEmailException;
import com.example.helloworld.exception.ValidationException;

/**
 * Contract for validating Employee data against business rules.
 *
 * <p>Implementations may check:
 * <ul>
 *   <li>Field-level rules (non-blank name, positive id, non-negative salary…)</li>
 *   <li>Uniqueness — email must not already belong to another employee</li>
 *   <li>Referential integrity — departmentId must correspond to a known department</li>
 * </ul>
 */
public interface ValidationService {

    /**
     * Validate all fields of the given employee.
     *
     * @throws ValidationException         on field-rule violations or inactive status
     * @throws DuplicateEmailException     when the email is already registered to a different employee
     * @throws DepartmentNotFoundException when the departmentId does not match a known department
     */
    void validate(Employee employee)
            throws ValidationException, DuplicateEmailException, DepartmentNotFoundException;
}
