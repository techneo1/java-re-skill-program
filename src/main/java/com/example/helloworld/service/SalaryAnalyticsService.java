package com.example.helloworld.service;

import com.example.helloworld.domain.DepartmentSalaryReport;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.SalaryAnalyticsReport;

import java.util.List;
import java.util.Map;

/**
 * Service interface for salary analytics.
 *
 * <p>All methods operate on a supplied employee list so the service is
 * stateless and easily testable — no repository coupling needed here.
 *
 * <p>Every implementation must use <strong>stream pipelines</strong>
 * (no manual loops) to compute aggregates.
 */
public interface SalaryAnalyticsService {

    /**
     * Groups employees by department id and returns a per-department salary
     * report (headcount, total, average, min, max salary).
     *
     * @param employees source list (may be empty)
     * @return unmodifiable map: departmentId → {@link DepartmentSalaryReport}
     */
    Map<Integer, DepartmentSalaryReport> groupByDepartment(List<Employee> employees);

    /**
     * Returns the top-N employees sorted by salary descending.
     *
     * @param employees source list (may be empty)
     * @param n         maximum number of results
     * @return unmodifiable list, at most {@code n} elements
     */
    List<Employee> topNBySalary(List<Employee> employees, int n);

    /**
     * Computes average salary grouped by role name (case-insensitive normalised to lower-case).
     *
     * @param employees source list (may be empty)
     * @return unmodifiable map: role → average salary
     */
    Map<String, Double> averageSalaryByRole(List<Employee> employees);

    /**
     * Partitions employees into active / inactive buckets.
     *
     * @param employees source list (may be empty)
     * @return two-entry map: {@code true} → active list, {@code false} → inactive list
     */
    Map<Boolean, List<Employee>> partitionByStatus(List<Employee> employees);

    /**
     * Convenience method — runs all four analytics and bundles the results.
     *
     * @param employees source list (may be empty)
     * @return a fully-populated {@link SalaryAnalyticsReport}
     */
    SalaryAnalyticsReport buildReport(List<Employee> employees);
}

