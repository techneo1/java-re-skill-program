package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.domain.payroll.PermanentEmployeePayrollStrategy;
import com.example.helloworld.exception.PayrollException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PayrollStrategyRegistry")
class PayrollStrategyRegistryTest {

    @Test
    @DisplayName("resolve — returns registered strategy for exact type")
    void resolve_exactType_returnsStrategy() throws Exception {
        PayrollStrategyRegistry registry = new PayrollStrategyRegistry()
                .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy());

        Employee emp = new PermanentEmployee(1, "A", "a@example.com",
                1, "Role", 1, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);

        assertNotNull(registry.resolve(emp));
    }

    @Test
    @DisplayName("resolve — throws PayrollException when no strategy registered")
    void resolve_missing_throws() {
        PayrollStrategyRegistry registry = new PayrollStrategyRegistry();

        Employee emp = new PermanentEmployee(1, "A", "a@example.com",
                1, "Role", 1, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);

        assertThrows(PayrollException.class, () -> registry.resolve(emp));
    }
}

