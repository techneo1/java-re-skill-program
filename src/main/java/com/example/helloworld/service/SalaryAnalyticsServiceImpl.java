package com.example.helloworld.service;

import com.example.helloworld.domain.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stream-based implementation of {@link SalaryAnalyticsService}.
 *
 * <p>Key Java features demonstrated throughout:
 * <ul>
 *   <li><b>Stream pipelines</b>  — {@code filter}, {@code sorted}, {@code groupingBy},
 *       {@code partitioningBy}, {@code collectingAndThen}, {@code summarizingDouble}</li>
 *   <li><b>Switch expressions</b> over the <b>sealed class hierarchy</b>
 *       ({@code Employee} → {@code PermanentEmployee} | {@code ContractEmployee})
 *       to derive type-specific values with compiler-enforced exhaustiveness</li>
 *   <li><b>Records as DTOs</b>   — {@link EmployeeSummaryDTO} and
 *       {@link DepartmentSalaryReport} carry results without mutable state</li>
 * </ul>
 */
public class SalaryAnalyticsServiceImpl implements SalaryAnalyticsService {

    private final EmployeeService employeeService;

    public SalaryAnalyticsServiceImpl(EmployeeService employeeService) {
        this.employeeService = Objects.requireNonNull(
                employeeService, "employeeService must not be null");
    }

    // ── 1 · Group by department ───────────────────────────────────────────────

    /**
     * Groups all employees by {@code departmentId} and computes per-department
     * salary statistics using a single Stream pipeline with
     * {@link Collectors#groupingBy} + {@link Collectors#summarizingDouble}.
     *
     * <pre>{@code
     * employees.stream()
     *     .collect(groupingBy(Employee::getDepartmentId,
     *              summarizingDouble(Employee::getSalary)))
     * }</pre>
     *
     * The resulting {@link DoubleSummaryStatistics} for each department provides
     * count, sum, average, min and max without any explicit loop.
     */
    @Override
    public Map<Integer, DepartmentSalaryReport> salaryByDepartment() {
        // Step 1: collect DoubleSummaryStatistics per departmentId
        Map<Integer, DoubleSummaryStatistics> statsMap =
                employeeService.getAllEmployees().stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDepartmentId,
                                TreeMap::new,
                                Collectors.summarizingDouble(Employee::getSalary)
                        ));

        // Step 2: project each entry into a DepartmentSalaryReport record
        return statsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int deptId = e.getKey();
                            DoubleSummaryStatistics s = e.getValue();
                            return new DepartmentSalaryReport(
                                    deptId,
                                    s.getCount(),
                                    s.getSum(),
                                    s.getCount() > 0 ? s.getAverage() : 0.0,
                                    s.getCount() > 0 ? s.getMin()     : 0.0,
                                    s.getCount() > 0 ? s.getMax()     : 0.0
                            );
                        },
                        (a, b) -> a,
                        TreeMap::new
                ));
    }

    // ── 2 · Top-N by salary ───────────────────────────────────────────────────

    /**
     * Sorts employees by salary descending, takes the first {@code n},
     * and projects each to an {@link EmployeeSummaryDTO} via
     * {@link EmployeeSummaryDTO#from(Employee)}.
     *
     * <p>{@code EmployeeSummaryDTO.from()} uses a <b>switch expression</b>
     * over the sealed hierarchy to derive the type tag:
     * <pre>{@code
     * String tag = switch (employee) {
     *     case PermanentEmployee pe -> "PERMANENT";
     *     case ContractEmployee  ce -> "CONTRACT";
     * };
     * }</pre>
     */
    @Override
    public List<EmployeeSummaryDTO> topNBySalary(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be > 0, got: " + n);

        return employeeService.getAllEmployees().stream()
                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .limit(n)
                .map(EmployeeSummaryDTO::from)           // switch expr inside from()
                .collect(Collectors.toUnmodifiableList());
    }

    // ── 3 · Average salary per role ───────────────────────────────────────────

    /**
     * Groups all employees by role and computes the average salary per group.
     *
     * <pre>{@code
     * employees.stream()
     *     .collect(groupingBy(Employee::getRole,
     *              averagingDouble(Employee::getSalary)))
     * }</pre>
     *
     * The result map is sorted by role name for deterministic output.
     */
    @Override
    public Map<String, Double> averageSalaryByRole() {
        return employeeService.getAllEmployees().stream()
                .collect(Collectors.groupingBy(
                        Employee::getRole,
                        TreeMap::new,                                   // sorted by role name
                        Collectors.averagingDouble(Employee::getSalary)
                ));
    }

    // ── 4 · Partition active vs inactive ─────────────────────────────────────

    /**
     * Uses {@link Collectors#partitioningBy} to split employees into exactly
     * two buckets in a single pipeline pass — no filter + collect twice needed.
     *
     * <pre>{@code
     * employees.stream()
     *     .collect(partitioningBy(
     *         e -> e.getStatus() == EmployeeStatus.ACTIVE,
     *         mapping(EmployeeSummaryDTO::from, toUnmodifiableList())))
     * }</pre>
     *
     * {@code true}  → ACTIVE employees
     * {@code false} → INACTIVE employees
     */
    @Override
    public Map<Boolean, List<EmployeeSummaryDTO>> partitionByStatus() {
        return employeeService.getAllEmployees().stream()
                .collect(Collectors.partitioningBy(
                        e -> e.getStatus() == EmployeeStatus.ACTIVE,
                        Collectors.mapping(
                                EmployeeSummaryDTO::from,               // switch expr inside from()
                                Collectors.toUnmodifiableList()
                        )
                ));
    }

    // ── 5 · Average salary by employee type ──────────────────────────────────

    /**
     * Derives the employee type tag via a <b>switch expression</b> directly
     * inside the {@link Collectors#groupingBy} classifier, then averages
     * salaries per group.
     *
     * <p>The switch expression on the <b>sealed hierarchy</b> is exhaustive —
     * no {@code default} branch is needed, and adding a new {@code permits}
     * subtype without updating this switch is a <em>compile error</em>.
     *
     * <pre>{@code
     * .collect(groupingBy(
     *     emp -> switch (emp) {              // ← switch expr on sealed Employee
     *         case PermanentEmployee pe -> "PERMANENT";
     *         case ContractEmployee  ce -> "CONTRACT";
     *     },
     *     averagingDouble(Employee::getSalary)
     * ))
     * }</pre>
     */
    @Override
    public Map<String, Double> averageSalaryByEmployeeType() {
        return employeeService.getAllEmployees().stream()
                .collect(Collectors.groupingBy(
                        emp -> switch (emp) {               // exhaustive switch expr
                            case PermanentEmployee pe -> "PERMANENT";
                            case ContractEmployee  ce -> "CONTRACT";
                        },
                        TreeMap::new,
                        Collectors.averagingDouble(Employee::getSalary)
                ));
    }
}
