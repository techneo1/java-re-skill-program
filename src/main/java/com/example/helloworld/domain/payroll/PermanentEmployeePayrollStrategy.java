package com.example.helloworld.domain.payroll;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.exception.PayrollException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payroll strategy for permanent employees.
 * Tax rate: 20% of gross salary.
 */
public class PermanentEmployeePayrollStrategy implements PayrollStrategy {

    private static final double TAX_RATE = 0.20;

    @Override
    public PayrollRecord calculate(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollException {
        if (!(employee instanceof PermanentEmployee)) {
            throw new PayrollException(employee.getId(),
                    "PermanentEmployeePayrollStrategy cannot process: " + employee.getEmployeeType());
        }
        double gross = employee.getSalary();
        double tax   = Math.round(gross * TAX_RATE * 100.0) / 100.0;
        return PayrollRecord.of(recordId, employee.getId(), gross, tax,
                payrollMonth, LocalDateTime.now());
    }
}

