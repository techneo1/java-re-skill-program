package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.payroll.PayrollStrategy;
import com.example.helloworld.exception.PayrollException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory registry mapping an Employee runtime type to a PayrollStrategy.
 */
public class PayrollStrategyRegistry implements PayrollStrategyResolver {

    private final Map<Class<?>, PayrollStrategy> strategies = new ConcurrentHashMap<>();

    public PayrollStrategyRegistry register(Class<? extends Employee> employeeType, PayrollStrategy strategy) {
        Objects.requireNonNull(employeeType, "employeeType must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");
        strategies.put(employeeType, strategy);
        return this;
    }

    @Override
    public PayrollStrategy resolve(Employee employee) throws PayrollException {
        Objects.requireNonNull(employee, "employee must not be null");

        PayrollStrategy exact = strategies.get(employee.getClass());
        if (exact != null) return exact;

        // Convenience: allow a strategy registered for a parent class/interface.
        for (Map.Entry<Class<?>, PayrollStrategy> e : strategies.entrySet()) {
            if (e.getKey().isAssignableFrom(employee.getClass())) {
                return e.getValue();
            }
        }

        throw new PayrollException(employee.getId(),
                "No payroll strategy registered for type: " + employee.getEmployeeType());
    }
}
