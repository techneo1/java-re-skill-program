package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.exception.ValidationException;

/**
 * Contract for validating Employee data against business rules.
 */
public interface ValidationService {

    /**
     * Validate all fields of the given employee.
     * Throws ValidationException on the first rule violation found.
     */
    void validate(Employee employee) throws ValidationException;
}

