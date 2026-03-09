package com.example.helloworld.service;

import com.example.helloworld.domain.ContractEmployee;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.domain.payroll.ContractEmployeePayrollStrategy;
import com.example.helloworld.domain.payroll.PayrollStrategy;
import com.example.helloworld.domain.payroll.PermanentEmployeePayrollStrategy;
import com.example.helloworld.exception.PayrollException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory registry mapping an Employee runtime type to a PayrollStrategy.
 *
 * Singleton pattern (Initialization-on-demand holder):
 *   A single, fully-wired instance is shared across the application via
 *   {@link #getInstance()}.  Tests that need an empty registry can still call
 *   {@code new PayrollStrategyRegistry()} directly — the public constructor is
 *   preserved to keep the class testable in isolation.
 */
public class PayrollStrategyRegistry implements PayrollStrategyResolver {

    // ── Singleton — Initialization-on-demand holder ───────────────────────────
    private static final class SingletonHolder {
        private static final PayrollStrategyRegistry INSTANCE =
                new PayrollStrategyRegistry()
                        .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy())
                        .register(ContractEmployee.class,  new ContractEmployeePayrollStrategy());
    }

    /**
     * Returns the application-wide, fully-wired singleton instance.
     * Thread-safe; lazy-initialised on first call.
     */
    public static PayrollStrategyRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // ── Instance state ────────────────────────────────────────────────────────
    private final Map<Class<?>, PayrollStrategy> strategies = new ConcurrentHashMap<>();

    public PayrollStrategyRegistry register(Class<? extends Employee> employeeType,
                                            PayrollStrategy strategy) {
        Objects.requireNonNull(employeeType, "employeeType must not be null");
        Objects.requireNonNull(strategy,     "strategy must not be null");
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
