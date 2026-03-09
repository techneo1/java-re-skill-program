package com.example.helloworld.service;

import com.example.helloworld.domain.DepartmentSalaryReport;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.SalaryAnalyticsReport;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stream-pipeline implementation of {@link SalaryAnalyticsService}.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>No mutable state — every method is a pure function over the supplied list.</li>
 *   <li>All results are wrapped in unmodifiable views so callers cannot corrupt them.</li>
 *   <li>Every aggregation is expressed as a single stream pipeline (no loops).</li>
 * </ul>
 */
public class SalaryAnalyticsServiceImpl implements SalaryAnalyticsService {

    // ── 1. Group by Department ───────────────────────────────────────────────��

    /**
     * Groups the employee list by {@code departmentId} and builds a
     * {@link DepartmentSalaryReport} for each group using a single
     * {@code Collectors.groupingBy} pipeline.
     */
    @Override
    public Map<Integer, DepartmentSalaryReport> groupByDepartment(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Employee::getDepartmentId),
                        grouped -> grouped.entrySet().stream()
                                .collect(Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        e -> DepartmentSalaryReport.of(e.getKey(), e.getValue())
                                ))
                ));
    }

    // ── 2. Top-N by salary ────────────────────────────────────────────────────

    /**
     * Sorts descending by salary, limits to {@code n}, collects into an
     * unmodifiable list — one stream pipeline, no loops.
     */
    @Override
    public List<Employee> topNBySalary(List<Employee> employees, int n) {
        if (n <= 0) return Collections.emptyList();
        return employees.stream()
                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .limit(n)
                .collect(Collectors.toUnmodifiableList());
    }

    // ── 3. Average salary per role ────────────────────────────────────────────

    /**
     * Groups by lower-cased role, then downstream-averages the salary.
     * Uses {@code Collectors.groupingBy} + {@code Collectors.averagingDouble}.
     */
    @Override
    public Map<String, Double> averageSalaryByRole(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                e -> e.getRole().strip().toLowerCase(),
                                Collectors.averagingDouble(Employee::getSalary)
                        ),
                        Collections::unmodifiableMap
                ));
    }

    // ── 4. Partition active / inactive ────────────────────────────────────────

    /**
     * Uses {@code Collectors.partitioningBy} — a specialised two-bucket
     * collector that is more efficient than {@code groupingBy} for boolean
     * predicates.
     *
     * @return map with key {@code true} → ACTIVE list, {@code false} → INACTIVE list
     */
    @Override
    public Map<Boolean, List<Employee>> partitionByStatus(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.partitioningBy(
                        e -> e.getStatus() == EmployeeStatus.ACTIVE,
                        Collectors.toUnmodifiableList()
                ));
    }

    // ── 5. Convenience report builder ─────────────────────────────────────────

    /**
     * Runs all four analytics in one pass and bundles them into a
     * {@link SalaryAnalyticsReport}.
     *
     * <p>The partition result is destructured so the report carries typed
     * {@code activeEmployees} / {@code inactiveEmployees} lists rather than
     * an opaque {@code Map<Boolean, List>}.
     */
    @Override
    public SalaryAnalyticsReport buildReport(List<Employee> employees) {
        Map<Integer, DepartmentSalaryReport> byDept      = groupByDepartment(employees);
        List<Employee>                       top5        = topNBySalary(employees, 5);
        Map<String, Double>                  avgByRole   = averageSalaryByRole(employees);
        Map<Boolean, List<Employee>>         partition   = partitionByStatus(employees);

        return new SalaryAnalyticsReport(
                byDept,
                top5,
                avgByRole,
                partition.get(true),   // ACTIVE
                partition.get(false)   // INACTIVE
        );
    }
}

