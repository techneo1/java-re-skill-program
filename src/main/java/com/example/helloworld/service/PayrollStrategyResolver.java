package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.payroll.PayrollStrategy;
import com.example.helloworld.exception.PayrollException;

/**
 * Resolves a {@link PayrollStrategy} for a given employee.
 *
 * SOLID:
 * - DIP: PayrollService depends on this abstraction, not on concrete strategy types.
 * - OCP: add support for new employee types by registering a new strategy.
 */
public interface PayrollStrategyResolver {

    PayrollStrategy resolve(Employee employee) throws PayrollException;
}

