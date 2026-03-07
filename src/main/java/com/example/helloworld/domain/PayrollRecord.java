package com.example.helloworld.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record PayrollRecord(
        int id,
        int employeeId,
        double grossSalary,
        double taxAmount,
        double netSalary,
        LocalDate payrollMonth,
        LocalDateTime processedTimestamp
) {
    public PayrollRecord {
        Objects.requireNonNull(payrollMonth,       "payrollMonth must not be null");
        Objects.requireNonNull(processedTimestamp, "processedTimestamp must not be null");
        if (id <= 0)         throw new IllegalArgumentException("id must be positive");
        if (employeeId <= 0) throw new IllegalArgumentException("employeeId must be positive");
        if (grossSalary < 0) throw new IllegalArgumentException("grossSalary must not be negative");
        if (taxAmount < 0)   throw new IllegalArgumentException("taxAmount must not be negative");
        if (taxAmount > grossSalary)
            throw new IllegalArgumentException("taxAmount cannot exceed grossSalary");
        double expectedNet = grossSalary - taxAmount;
        if (Math.abs(netSalary - expectedNet) > 0.001)
            throw new IllegalArgumentException(
                    String.format("netSalary (%.2f) must equal grossSalary - taxAmount (%.2f)",
                            netSalary, expectedNet));
    }

    public static PayrollRecord of(int id, int employeeId, double grossSalary, double taxAmount,
                                   LocalDate payrollMonth, LocalDateTime processedTimestamp) {
        return new PayrollRecord(id, employeeId, grossSalary, taxAmount,
                grossSalary - taxAmount, payrollMonth, processedTimestamp);
    }
}

