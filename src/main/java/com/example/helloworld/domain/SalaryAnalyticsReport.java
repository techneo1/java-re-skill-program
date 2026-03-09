package com.example.helloworld.domain;

import java.util.List;
import java.util.Map;

/**
 * Immutable analytics report produced by {@link com.example.helloworld.service.SalaryAnalyticsService}.
 *
 * <p>Aggregates five analytics views in one place:
 * <ol>
 *   <li>{@code byDepartment}      — per-department salary summary (headcount, avg, min, max)</li>
 *   <li>{@code top5BySalary}      — up to 5 employees with the highest base salaries</li>
 *   <li>{@code avgSalaryByRole}   — average salary keyed by role name</li>
 *   <li>{@code activeEmployees}   — employees whose status is ACTIVE</li>
 *   <li>{@code inactiveEmployees} — employees whose status is INACTIVE</li>
 * </ol>
 *
 * <p>All collections are unmodifiable; the record itself is immutable.
 */
public record SalaryAnalyticsReport(
        Map<Integer, DepartmentSalaryReport> byDepartment,
        List<Employee>                       top5BySalary,
        Map<String, Double>                  avgSalaryByRole,
        List<Employee>                       activeEmployees,
        List<Employee>                       inactiveEmployees
) {
    /** Compact constructor — guards against null collections. */
    public SalaryAnalyticsReport {
        if (byDepartment      == null) throw new IllegalArgumentException("byDepartment must not be null");
        if (top5BySalary      == null) throw new IllegalArgumentException("top5BySalary must not be null");
        if (avgSalaryByRole   == null) throw new IllegalArgumentException("avgSalaryByRole must not be null");
        if (activeEmployees   == null) throw new IllegalArgumentException("activeEmployees must not be null");
        if (inactiveEmployees == null) throw new IllegalArgumentException("inactiveEmployees must not be null");
    }
}

