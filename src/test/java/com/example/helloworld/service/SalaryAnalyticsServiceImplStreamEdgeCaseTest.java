package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Stream edge-case tests for {@link SalaryAnalyticsServiceImpl}.
 *
 * Focuses on unusual but valid inputs that expose gaps in pipeline logic:
 * <ul>
 *   <li>Duplicate / tied salaries in sort and grouping</li>
 *   <li>All employees in a single department / single role</li>
 *   <li>All employees of the same type (only PERMANENT, only CONTRACT)</li>
 *   <li>Large roster (100 employees) — pipeline must not lose records</li>
 *   <li>Zero-salary employees — min/max/avg must still be correct</li>
 *   <li>Return-value immutability — results cannot be mutated by callers</li>
 *   <li>Pipeline is stateless — calling the same method twice returns consistent results</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAnalyticsServiceImpl — Stream edge cases")
class SalaryAnalyticsServiceImplStreamEdgeCaseTest {

    @Mock  private EmployeeService employeeService;
    @InjectMocks private SalaryAnalyticsServiceImpl analytics;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee perm(int id, String role, double salary,
                                          EmployeeStatus status, int deptId) {
        return new PermanentEmployee(id, "Emp" + id, "emp" + id + "@x.com",
                deptId, role, salary, status,
                LocalDate.of(2020, 1, 1), false);
    }

    private static ContractEmployee contract(int id, String role, double salary, int deptId) {
        return new ContractEmployee(id, "Emp" + id, "emp" + id + "@x.com",
                deptId, role, salary, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2027, 12, 31));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tied / duplicate salaries
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("topNBySalary — all employees have identical salary, all N returned")
    void topNBySalary_allTiedSalaries_returnsN() {
        List<Employee> employees = List.of(
                perm(1, "Eng", 80_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "Eng", 80_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "Eng", 80_000, EmployeeStatus.ACTIVE, 10)
        );
        when(employeeService.getAllEmployees()).thenReturn(employees);

        List<EmployeeSummaryDTO> top2 = analytics.topNBySalary(2);
        assertEquals(2, top2.size());
        // both must have the tied salary
        assertTrue(top2.stream().allMatch(e -> e.salary() == 80_000));
    }

    @Test
    @DisplayName("topNBySalary — tied first place: top-1 still returns exactly 1 result")
    void topNBySalary_tiedFirst_returnsExactlyOne() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng", 100_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "Mgr", 100_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "Eng",  80_000, EmployeeStatus.ACTIVE, 10)
        ));
        assertEquals(1, analytics.topNBySalary(1).size());
        assertEquals(100_000, analytics.topNBySalary(1).get(0).salary(), 0.01);
    }

    @Test
    @DisplayName("salaryByDepartment — all employees with same salary: min==max==avg==salary")
    void salaryByDepartment_uniformSalaries_minMaxAvgEqual() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng", 70_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "Mgr", 70_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "Eng", 70_000, EmployeeStatus.ACTIVE, 10)
        ));
        DepartmentSalaryReport r = analytics.salaryByDepartment().get(10);
        assertEquals(70_000, r.minSalary(),     0.01);
        assertEquals(70_000, r.maxSalary(),     0.01);
        assertEquals(70_000, r.averageSalary(), 0.01);
        assertEquals(3,      r.headCount());
    }

    @Test
    @DisplayName("averageSalaryByRole — two roles with identical average salaries appear separately")
    void averageSalaryByRole_twoRolesSameAverage_appearsAsDistinctKeys() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Engineer", 60_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "Manager",  60_000, EmployeeStatus.ACTIVE, 10)
        ));
        Map<String, Double> result = analytics.averageSalaryByRole();
        assertEquals(2, result.size());
        assertEquals(60_000, result.get("Engineer"), 0.01);
        assertEquals(60_000, result.get("Manager"),  0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // All employees in a single department / single role
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("salaryByDepartment — all employees in one dept → exactly one map entry")
    void salaryByDepartment_allInOneDept_singleEntry() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 50_000, EmployeeStatus.ACTIVE, 5),
                perm(2, "E", 70_000, EmployeeStatus.ACTIVE, 5),
                perm(3, "E", 90_000, EmployeeStatus.ACTIVE, 5)
        ));
        Map<Integer, DepartmentSalaryReport> result = analytics.salaryByDepartment();
        assertEquals(1, result.size());
        assertTrue(result.containsKey(5));
        assertEquals(3, result.get(5).headCount());
        assertEquals(50_000, result.get(5).minSalary(), 0.01);
        assertEquals(90_000, result.get(5).maxSalary(), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — all employees have same role → exactly one map entry")
    void averageSalaryByRole_allSameRole_singleEntry() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Engineer", 60_000, EmployeeStatus.ACTIVE,   10),
                perm(2, "Engineer", 80_000, EmployeeStatus.ACTIVE,   20),
                perm(3, "Engineer", 70_000, EmployeeStatus.INACTIVE, 30)
        ));
        Map<String, Double> result = analytics.averageSalaryByRole();
        assertEquals(1, result.size());
        assertTrue(result.containsKey("Engineer"));
        assertEquals(70_000, result.get("Engineer"), 0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // All employees of the same type
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("averageSalaryByEmployeeType — only PERMANENT employees → no CONTRACT key")
    void averageSalaryByEmployeeType_onlyPermanent_contractKeyAbsent() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 80_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "E", 90_000, EmployeeStatus.ACTIVE, 10)
        ));
        Map<String, Double> result = analytics.averageSalaryByEmployeeType();
        assertEquals(1, result.size());
        assertTrue(result.containsKey("PERMANENT"));
        assertFalse(result.containsKey("CONTRACT"));
    }

    @Test
    @DisplayName("averageSalaryByEmployeeType — only CONTRACT employees → no PERMANENT key")
    void averageSalaryByEmployeeType_onlyContract_permanentKeyAbsent() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                contract(1, "C", 50_000, 10),
                contract(2, "C", 70_000, 10)
        ));
        Map<String, Double> result = analytics.averageSalaryByEmployeeType();
        assertEquals(1, result.size());
        assertTrue(result.containsKey("CONTRACT"));
        assertFalse(result.containsKey("PERMANENT"));
    }

    @Test
    @DisplayName("partitionByStatus — all INACTIVE → active bucket empty, inactive has all")
    void partitionByStatus_allInactive_activeBucketEmpty() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 80_000, EmployeeStatus.INACTIVE, 10),
                perm(2, "E", 70_000, EmployeeStatus.INACTIVE, 10)
        ));
        Map<Boolean, List<EmployeeSummaryDTO>> result = analytics.partitionByStatus();
        assertTrue(result.get(true).isEmpty());
        assertEquals(2, result.get(false).size());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Zero-salary employees
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("salaryByDepartment — zero-salary employee: min/avg/total all zero")
    void salaryByDepartment_zeroSalary_statsAreZero() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Intern", 0, EmployeeStatus.ACTIVE, 99)
        ));
        DepartmentSalaryReport r = analytics.salaryByDepartment().get(99);
        assertEquals(0.0, r.totalSalary(),   0.001);
        assertEquals(0.0, r.averageSalary(), 0.001);
        assertEquals(0.0, r.minSalary(),     0.001);
        assertEquals(0.0, r.maxSalary(),     0.001);
    }

    @Test
    @DisplayName("averageSalaryByRole — zero-salary role average is 0.0")
    void averageSalaryByRole_zeroSalary_averageIsZero() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Intern", 0, EmployeeStatus.ACTIVE, 10)
        ));
        assertEquals(0.0, analytics.averageSalaryByRole().get("Intern"), 0.001);
    }

    @Test
    @DisplayName("topNBySalary — zero-salary employee appears last when others have positive salary")
    void topNBySalary_zeroSalaryEmployeeLast() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng",    80_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "Intern",      0, EmployeeStatus.ACTIVE, 10)
        ));
        List<EmployeeSummaryDTO> top = analytics.topNBySalary(2);
        assertEquals(80_000, top.get(0).salary(), 0.01);
        assertEquals(0,      top.get(1).salary(), 0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Large roster (100 employees)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("topNBySalary — 100 employees: top-10 all have salaries ≥ 91st employee")
    void topNBySalary_largeRoster_top10AreHighest() {
        List<Employee> employees = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> (Employee) perm(i, "Eng", i * 1_000.0, EmployeeStatus.ACTIVE, 10))
                .toList();
        when(employeeService.getAllEmployees()).thenReturn(employees);

        List<EmployeeSummaryDTO> top10 = analytics.topNBySalary(10);
        assertEquals(10, top10.size());
        assertEquals(100_000, top10.get(0).salary(), 0.01);
        assertEquals(91_000,  top10.get(9).salary(), 0.01);
        assertTrue(top10.stream().allMatch(e -> e.salary() >= 91_000));
    }

    @Test
    @DisplayName("salaryByDepartment — 100 employees spread across 10 depts: each dept has 10")
    void salaryByDepartment_largeRoster_headCountPerDeptCorrect() {
        List<Employee> employees = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> (Employee) perm(i, "Eng", 50_000, EmployeeStatus.ACTIVE,
                        ((i - 1) % 10) + 1))
                .toList();
        when(employeeService.getAllEmployees()).thenReturn(employees);

        Map<Integer, DepartmentSalaryReport> result = analytics.salaryByDepartment();
        assertEquals(10, result.size());
        result.values().forEach(r -> assertEquals(10, r.headCount()));
    }

    @Test
    @DisplayName("averageSalaryByRole — 100 employees all same role: avg = arithmetic mean")
    void averageSalaryByRole_largeRoster_averageIsCorrect() {
        List<Employee> employees = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> (Employee) perm(i, "Eng", i * 1_000.0, EmployeeStatus.ACTIVE, 10))
                .toList();
        when(employeeService.getAllEmployees()).thenReturn(employees);

        double expected = IntStream.rangeClosed(1, 100).sum() * 1_000.0 / 100;  // 50_500
        assertEquals(expected, analytics.averageSalaryByRole().get("Eng"), 0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Return-value immutability
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("topNBySalary — returned list is unmodifiable")
    void topNBySalary_returnedList_isUnmodifiable() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 80_000, EmployeeStatus.ACTIVE, 10)
        ));
        List<EmployeeSummaryDTO> result = analytics.topNBySalary(1);
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }

    @Test
    @DisplayName("partitionByStatus — active sub-list is unmodifiable")
    void partitionByStatus_activeList_isUnmodifiable() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 80_000, EmployeeStatus.ACTIVE, 10)
        ));
        List<EmployeeSummaryDTO> active = analytics.partitionByStatus().get(true);
        assertThrows(UnsupportedOperationException.class, () -> active.add(null));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Pipeline statelessness — calling twice gives consistent results
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("salaryByDepartment — calling twice returns same values (stateless pipeline)")
    void salaryByDepartment_calledTwice_consistentResults() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 80_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "E", 90_000, EmployeeStatus.ACTIVE, 10)
        ));
        DepartmentSalaryReport first  = analytics.salaryByDepartment().get(10);
        DepartmentSalaryReport second = analytics.salaryByDepartment().get(10);
        assertEquals(first, second);
    }

    @Test
    @DisplayName("averageSalaryByRole — calling twice returns same map (stateless pipeline)")
    void averageSalaryByRole_calledTwice_consistentResults() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng", 80_000, EmployeeStatus.ACTIVE, 10)
        ));
        Map<String, Double> first  = analytics.averageSalaryByRole();
        Map<String, Double> second = analytics.averageSalaryByRole();
        assertEquals(first, second);
    }
}
