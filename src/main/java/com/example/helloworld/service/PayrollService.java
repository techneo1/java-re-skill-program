package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;

import java.time.LocalDate;
import java.util.List;

/**
 * Contract for payroll processing operations.
 */
public interface PayrollService {

    /**
     * Process payroll for a single employee for the given month.
     */
    PayrollRecord processPayroll(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollException;

    /**
     * Process payroll for all given employees for the given month.
     * Employees that fail are skipped and their errors are collected — processing continues.
     */
    List<PayrollRecord> processAll(List<Employee> employees, LocalDate payrollMonth);
}

