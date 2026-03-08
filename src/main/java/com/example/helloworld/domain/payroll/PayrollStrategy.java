package com.example.helloworld.domain.payroll;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;

import java.time.LocalDate;

/**
 * Strategy interface for payroll calculation.
 * Each employee type provides its own implementation.
 */
public interface PayrollStrategy {

    /**
     * Compute a PayrollRecord for the given employee and payroll month.
     *
     * @param recordId     unique id for the resulting PayrollRecord
     * @param employee     the employee to process
     * @param payrollMonth the month being processed (day component is ignored)
     * @return a fully populated PayrollRecord
     * @throws PayrollException if the employee cannot be processed (e.g. expired contract)
     */
    PayrollRecord calculate(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollException;
}

