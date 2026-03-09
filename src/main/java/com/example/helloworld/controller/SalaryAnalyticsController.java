package com.example.helloworld.controller;

import com.example.helloworld.domain.DepartmentSalaryReport;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.SalaryAnalyticsReport;
import com.example.helloworld.service.EmployeeService;
import com.example.helloworld.service.SalaryAnalyticsService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controller layer for salary analytics.
 *
 * <p>Orchestrates the flow:
 * <pre>
 *   SalaryAnalyticsController
 *       → EmployeeService  (fetches employees)
 *       → SalaryAnalyticsService  (computes analytics via stream pipelines)
 * </pre>
 *
 * <p>All methods catch any unexpected runtime exception so the caller is
 * never exposed to a stack trace — consistent with the rest of the controller
 * layer in this project.
 */
public class SalaryAnalyticsController {

    private final EmployeeService        employeeService;
    private final SalaryAnalyticsService analyticsService;

    public SalaryAnalyticsController(EmployeeService employeeService,
                                     SalaryAnalyticsService analyticsService) {
        this.employeeService  = Objects.requireNonNull(employeeService,  "employeeService must not be null");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService must not be null");
    }

    // ── 1. Group employees by department ─────────────────────────────────────

    /**
     * Returns a per-department salary summary for all employees currently
     * stored in the repository.
     *
     * @return unmodifiable map: departmentId → {@link DepartmentSalaryReport};
     *         empty map on error
     */
    public Map<Integer, DepartmentSalaryReport> groupByDepartment() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.groupByDepartment(all);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error grouping by department: %s%n", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── 2. Top-5 highest salaries ─────────────────────────────────────────────

    /**
     * Returns up to 5 employees with the highest base salaries, sorted
     * descending.
     *
     * @return unmodifiable list; empty list on error
     */
    public List<Employee> top5BySalary() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.topNBySalary(all, 5);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error fetching top 5 salaries: %s%n", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 3. Average salary per role ────────────────────────────────────────────

    /**
     * Returns the average salary grouped by role name (lower-cased).
     *
     * @return unmodifiable map: role → average salary; empty map on error
     */
    public Map<String, Double> averageSalaryByRole() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.averageSalaryByRole(all);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error computing avg salary by role: %s%n", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── 4. Partition active vs inactive ──────────────────────────────────────

    /**
     * Partitions all employees into ACTIVE / INACTIVE buckets.
     *
     * @return map: {@code true} → active list, {@code false} → inactive list;
     *         both keys always present (lists may be empty)
     */
    public Map<Boolean, List<Employee>> partitionByStatus() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.partitionByStatus(all);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error partitioning by status: %s%n", e.getMessage());
            return Map.of(true, Collections.emptyList(), false, Collections.emptyList());
        }
    }

    // ── 5. Group employees by role ────────────────────────────────────────────

    /**
     * Groups all employees by role name (lower-cased).
     * Within each role bucket employees are sorted salary desc → name asc.
     *
     * @return unmodifiable map: role → employee list; empty map on error
     */
    public Map<String, List<Employee>> groupByRole() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.groupByRole(all);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error grouping by role: %s%n", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── 6. Full analytics report ──────────────────────────────────────────────

    /**
     * Runs all four analytics and returns a bundled {@link SalaryAnalyticsReport}.
     *
     * @return fully-populated report; {@code null} on error (logged to stderr)
     */
    public SalaryAnalyticsReport buildReport() {
        try {
            List<Employee> all = employeeService.getAllEmployees();
            return analyticsService.buildReport(all);
        } catch (Exception e) {
            System.err.printf("[SalaryAnalyticsController] Error building analytics report: %s%n", e.getMessage());
            return null;
        }
    }
}
