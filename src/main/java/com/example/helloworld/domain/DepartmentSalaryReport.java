package com.example.helloworld.domain;

import java.util.List;
import java.util.Objects;

/**
 * Immutable analytics report for a single department's salary data.
 *
 * Record gives us compact declaration, auto-generated equals/hashCode/toString,
 * and enforced immutability — ideal for a DTO that is only ever read, never
 * mutated after creation.
 *
 * Built via {@link #of(int, List)} which computes all aggregates from a
 * stream pipeline, so the caller never has to derive them manually.
 */
public record DepartmentSalaryReport(
        int    departmentId,
        int    headCount,
        double totalSalary,
        double averageSalary,
        double minSalary,
        double maxSalary
) {
    /** Compact constructor — guards against logically impossible aggregates. */
    public DepartmentSalaryReport {
        if (departmentId <= 0) throw new IllegalArgumentException("departmentId must be positive");
        if (headCount    <  0) throw new IllegalArgumentException("headCount must not be negative");
        if (totalSalary  <  0) throw new IllegalArgumentException("totalSalary must not be negative");
        if (averageSalary < 0) throw new IllegalArgumentException("averageSalary must not be negative");
        if (minSalary    <  0) throw new IllegalArgumentException("minSalary must not be negative");
        if (maxSalary    <  0) throw new IllegalArgumentException("maxSalary must not be negative");
    }

    /**
     * Builds the report for one department from a non-empty employee list.
     * All aggregates are computed via a single stream pass.
     *
     * @param departmentId the department being reported on
     * @param employees    non-null, non-empty list of employees in that department
     * @return a fully populated {@link DepartmentSalaryReport}
     */
    public static DepartmentSalaryReport of(int departmentId, List<Employee> employees) {
        Objects.requireNonNull(employees, "employees must not be null");
        if (employees.isEmpty())
            throw new IllegalArgumentException("employees must not be empty");

        var stats = employees.stream()
                .mapToDouble(Employee::getSalary)
                .summaryStatistics();

        return new DepartmentSalaryReport(
                departmentId,
                (int) stats.getCount(),
                stats.getSum(),
                stats.getAverage(),
                stats.getMin(),
                stats.getMax()
        );
    }
}

