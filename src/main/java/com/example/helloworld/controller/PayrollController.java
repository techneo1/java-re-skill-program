package com.example.helloworld.controller;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;
import com.example.helloworld.service.PayrollService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Controller layer for payroll operations.
 * Accepts requests, delegates to PayrollService,
 * and handles all checked exceptions — no exception leaks to the caller.
 *
 * Layer: Controller → Service → Repository
 */
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = Objects.requireNonNull(payrollService, "payrollService must not be null");
    }

    /**
     * Process payroll for a single employee.
     * Returns the PayrollRecord, or null if processing failed (error printed to stderr).
     */
    public PayrollRecord processPayroll(int recordId, Employee employee, LocalDate payrollMonth) {
        try {
            PayrollRecord record = payrollService.processPayroll(recordId, employee, payrollMonth);
            System.out.printf("[PayrollController] Processed: employeeId=%-3d  gross=%.2f  tax=%.2f  net=%.2f%n",
                    record.employeeId(), record.grossSalary(), record.taxAmount(), record.netSalary());
            return record;
        } catch (PayrollException e) {
            System.err.printf("[PayrollController] Payroll error for employeeId=%d: %s%n",
                    employee.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Process payroll for a batch of employees (fault-tolerant).
     * Failed employees are skipped; their errors are printed to stderr.
     */
    public List<PayrollRecord> processAll(List<Employee> employees, LocalDate payrollMonth) {
        Objects.requireNonNull(employees,    "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");
        List<PayrollRecord> records = payrollService.processAll(employees, payrollMonth);
        System.out.printf("[PayrollController] Batch complete: %d/%d records processed%n",
                records.size(), employees.size());
        return records;
    }
}

