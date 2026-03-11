package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Selects the correct PayrollStrategy per employee type and delegates calculation.
 * processAll() is fault-tolerant — failed employees are logged and skipped.
 */
public class PayrollServiceImpl implements PayrollService {

    private final PayrollStrategyResolver resolver;

    /**
     * Default wiring — uses the application-wide Singleton registry.
     * New employee types can be supported by registering another strategy
     * on {@link PayrollStrategyRegistry#getInstance()}.
     */
    public PayrollServiceImpl() {
        this(PayrollStrategyRegistry.getInstance());
    }

    public PayrollServiceImpl(PayrollStrategyResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public PayrollRecord processPayroll(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollException {
        Objects.requireNonNull(employee,     "employee must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");
        return resolver.resolve(employee).calculate(recordId, employee, payrollMonth);
    }

    @Override
    public List<PayrollRecord> processAll(List<Employee> employees, LocalDate payrollMonth) {
        Objects.requireNonNull(employees,    "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");

        List<PayrollRecord> results = new ArrayList<>();
        int recordId = 1;
        for (Employee emp : employees) {
            try {
                results.add(processPayroll(recordId++, emp, payrollMonth));
            } catch (PayrollException e) {
                System.err.println("[PayrollService] Skipped employee id=" + emp.getId()
                        + " — " + e.getMessage());
            }
        }
        return List.copyOf(results);
    }
}
