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
 *   <li>Comparator chaining is used in topNBySalary: primary sort = salary desc,
 *       tiebreaker = name asc (deterministic ordering for equal salaries).</li>
 * </ul>
 */
public class SalaryAnalyticsServiceImpl implements SalaryAnalyticsService {

    // ── Comparator chain (reusable constant) ──────────────────────────────────

    /**
     * Primary: salary descending.
     * Tiebreaker 1: name ascending (alphabetical).
     * Tiebreaker 2: id ascending (guarantees total ordering, no two employees share an id).
     *
     * Comparator chaining with .thenComparing() demonstrates how multiple
     * sort keys compose into a single Comparator without any if/else logic.
     */
    private static final Comparator<Employee> SALARY_DESC_THEN_NAME_ASC_THEN_ID =
            Comparator.comparingDouble(Employee::getSalary).reversed()
                      .thenComparing(Employee::getName)
                      .thenComparingInt(Employee::getId);

    // ── 1. Group by Department ────────────────────────────────────────────────

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

    // ── 2. Top-N by salary (with Comparator chaining) ─────────────────────────

    /**
     * Sorts using the chained Comparator (salary desc → name asc → id asc),
     * limits to {@code n}, and collects into an unmodifiable list.
     * The multi-key chain ensures a fully deterministic ordering even when
     * multiple employees share the same salary.
     */
    @Override
    public List<Employee> topNBySalary(List<Employee> employees, int n) {
        if (n <= 0) return Collections.emptyList();
        return employees.stream()
                .sorted(SALARY_DESC_THEN_NAME_ASC_THEN_ID)
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

    // ── 5. Group employees by role ────────────────────────────────────────────

    /**
     * Groups employees into per-role buckets (role name normalised to lower-case).
     * Within each bucket, employees are sorted by salary descending then name ascending
     * so the grouped results are presented in a consistent, meaningful order.
     *
     * Uses {@code Collectors.groupingBy} with a downstream
     * {@code Collectors.collectingAndThen} to sort + make unmodifiable.
     */
    @Override
    public Map<String, List<Employee>> groupByRole(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                e -> e.getRole().strip().toLowerCase(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            list.sort(SALARY_DESC_THEN_NAME_ASC_THEN_ID);
                                            return Collections.unmodifiableList(list);
                                        }
                                )
                        ),
                        Collections::unmodifiableMap
                ));
    }

    // ── 6. Convenience report builder ─────────────────────────────────────────

    /**
     * Runs all analytics and bundles them into a {@link SalaryAnalyticsReport}.
     * The partition result is destructured so the report carries typed
     * activeEmployees / inactiveEmployees lists.
     */
    @Override
    public SalaryAnalyticsReport buildReport(List<Employee> employees) {
        Map<Integer, DepartmentSalaryReport> byDept    = groupByDepartment(employees);
        List<Employee>                       top5      = topNBySalary(employees, 5);
        Map<String, Double>                  avgByRole = averageSalaryByRole(employees);
        Map<Boolean, List<Employee>>         partition = partitionByStatus(employees);
        Map<String, List<Employee>>          byRole    = groupByRole(employees);

        return new SalaryAnalyticsReport(
                byDept,
                top5,
                avgByRole,
                partition.get(true),   // ACTIVE
                partition.get(false),  // INACTIVE
                byRole
        );
    }
}
