package com.example.helloworld.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable analytics report produced by {@link com.example.helloworld.service.SalaryAnalyticsService}.
 *
 * <p>Aggregates six analytics views in one place:
 * <ol>
 *   <li>{@code byDepartment}      — per-department salary summary (headcount, avg, min, max)</li>
 *   <li>{@code top5BySalary}      — up to 5 employees with the highest base salaries</li>
 *   <li>{@code avgSalaryByRole}   — average salary keyed by role name</li>
 *   <li>{@code activeEmployees}   — employees whose status is ACTIVE</li>
 *   <li>{@code inactiveEmployees} — employees whose status is INACTIVE</li>
 *   <li>{@code byRole}            — employees grouped by role name</li>
 * </ol>
 *
 * <p>All collections are unmodifiable; the record itself is immutable.
 */
public record SalaryAnalyticsReport(
        Map<Integer, DepartmentSalaryReport> byDepartment,
        List<Employee>                       top5BySalary,
        Map<String, Double>                  avgSalaryByRole,
        List<Employee>                       activeEmployees,
        List<Employee>                       inactiveEmployees,
        Map<String, List<Employee>>          byRole
) {
    /** Compact constructor — guards against null collections. */
    public SalaryAnalyticsReport {
        Objects.requireNonNull(byDepartment,      "byDepartment must not be null");
        Objects.requireNonNull(top5BySalary,      "top5BySalary must not be null");
        Objects.requireNonNull(avgSalaryByRole,   "avgSalaryByRole must not be null");
        Objects.requireNonNull(activeEmployees,   "activeEmployees must not be null");
        Objects.requireNonNull(inactiveEmployees, "inactiveEmployees must not be null");
        Objects.requireNonNull(byRole,            "byRole must not be null");
    }
}
