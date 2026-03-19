package com.example.helloworld.service;

import com.example.helloworld.domain.DepartmentSalaryReport;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.EmployeeSummaryDTO;

import java.util.List;
import java.util.Map;

/**
 * Analytics contract for salary-related queries across the employee roster.
 *
 * <p>All operations are <b>read-only</b> — no state is mutated.
 * The implementation uses <b>Stream pipelines</b> (no explicit loops) and
 * <b>switch expressions</b> over the sealed {@link com.example.helloworld.domain.Employee}
 * hierarchy to derive type-specific values.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link SalaryAnalyticsServiceImpl} — pure in-memory Stream implementation</li>
 * </ul>
 */
public interface SalaryAnalyticsService {

    /**
     * Groups every employee by {@code departmentId} and computes per-department
     * salary statistics (head count, total, average, min, max).
     *
     * @return map keyed by {@code departmentId}; entries ordered by departmentId ascending
     */
    Map<Integer, DepartmentSalaryReport> salaryByDepartment();

    /**
     * Returns the top-N employees by salary, highest first.
     * If fewer than N employees exist, all are returned.
     *
     * @param n maximum number of results (must be &gt; 0)
     * @return immutable list of up to N {@link EmployeeSummaryDTO}s, sorted descending by salary
     */
    List<EmployeeSummaryDTO> topNBySalary(int n);

    /**
     * Convenience method — returns the top 5 employees by salary.
     * Equivalent to {@code topNBySalary(5)}.
     */
    default List<EmployeeSummaryDTO> top5BySalary() {
        return topNBySalary(5);
    }

    /**
     * Computes the average salary for each distinct role across all employees.
     *
     * @return map of {@code role → averageSalary}, entries ordered by role name ascending
     */
    Map<String, Double> averageSalaryByRole();

    /**
     * Partitions all employees into two groups:
     * <ul>
     *   <li>{@code true}  → {@link EmployeeStatus#ACTIVE} employees</li>
     *   <li>{@code false} → {@link EmployeeStatus#INACTIVE} employees</li>
     * </ul>
     *
     * @return map with exactly two keys ({@code true} and {@code false});
     *         each value is an immutable list of {@link EmployeeSummaryDTO}s
     */
    Map<Boolean, List<EmployeeSummaryDTO>> partitionByStatus();

    /**
     * Returns a salary breakdown per employee type tag
     * ({@code "PERMANENT"} / {@code "CONTRACT"}), using a
     * <b>switch expression</b> over the sealed class hierarchy to derive the tag.
     *
     * <p>The tag is computed by:
     * <pre>{@code
     * String tag = switch (employee) {
     *     case PermanentEmployee pe -> "PERMANENT";
     *     case ContractEmployee  ce -> "CONTRACT";
     * };
     * }</pre>
     *
     * @return map of {@code employeeType → averageSalary}
     */
    Map<String, Double> averageSalaryByEmployeeType();
}

