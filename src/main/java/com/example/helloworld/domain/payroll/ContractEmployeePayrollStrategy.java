package com.example.helloworld.domain.payroll;

import com.example.helloworld.domain.ContractEmployee;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payroll strategy for contract employees.
 * Tax rate: 10% of gross salary.
 * Throws PayrollException if the contract has already expired.
 */
public class ContractEmployeePayrollStrategy implements PayrollStrategy {

    private static final double TAX_RATE = 0.10;

    @Override
    public PayrollRecord calculate(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollException {
        if (!(employee instanceof ContractEmployee contractEmp)) {
            throw new PayrollException(employee.getId(),
                    "ContractEmployeePayrollStrategy cannot process: " + employee.getEmployeeType());
        }
        if (contractEmp.isExpired()) {
            throw new PayrollException(employee.getId(),
                    "Cannot process payroll: contract expired on " + contractEmp.getContractEndDate());
        }
        double gross = employee.getSalary();
        double tax   = Math.round(gross * TAX_RATE * 100.0) / 100.0;
        return PayrollRecord.of(recordId, employee.getId(), gross, tax,
                payrollMonth, LocalDateTime.now());
    }
}

