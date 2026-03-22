package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Focused tests for {@link SalaryAnalyticsServiceImpl#salaryByDepartment()}
 * with edge cases: single department, ties in salary, mixed employee types
 * in the same department.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAnalyticsServiceImpl — groupBy department edge cases")
class SalaryAnalyticsServiceImplGroupByRoleTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private SalaryAnalyticsServiceImpl analytics;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee perm(int id, int deptId, String role, double salary,
                                          EmployeeStatus status) {
        return new PermanentEmployee(id, "Employee " + id, "emp" + id + "@example.com",
                deptId, role, salary, status,
                LocalDate.of(2020, 1, 1), false);
    }

    private static ContractEmployee contract(int id, int deptId, String role, double salary) {
        return new ContractEmployee(id, "Employee " + id, "emp" + id + "@example.com",
                deptId, role, salary, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2026, 12, 31));
    }

    // ── salaryByDepartment: all in one department ─────────────────────────────

    @Test
    @DisplayName("all employees in same dept → single map entry")
    void salaryByDepartment_allSameDept_singleEntry() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, 10, "Eng", 80_000, EmployeeStatus.ACTIVE),
                perm(2, 10, "Eng", 90_000, EmployeeStatus.ACTIVE),
                perm(3, 10, "Eng", 70_000, EmployeeStatus.ACTIVE)
        ));
        Map<Integer, DepartmentSalaryReport> result = analytics.salaryByDepartment();
        assertEquals(1, result.size());
        DepartmentSalaryReport r = result.get(10);
        assertNotNull(r);
        assertEquals(3,       r.headCount());
        assertEquals(240_000, r.totalSalary(),   0.01);
        assertEquals(80_000,  r.averageSalary(),  0.01);
        assertEquals(70_000,  r.minSalary(),      0.01);
        assertEquals(90_000,  r.maxSalary(),      0.01);
    }

    @Test
    @DisplayName("mixed PERMANENT + CONTRACT in same dept → both counted")
    void salaryByDepartment_mixedTypes_bothCounted() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1,     10, "Eng",  80_000, EmployeeStatus.ACTIVE),
                contract(2, 10, "Cons", 60_000)
        ));
        DepartmentSalaryReport r = analytics.salaryByDepartment().get(10);
        assertEquals(2,       r.headCount());
        assertEquals(140_000, r.totalSalary(),  0.01);
        assertEquals(70_000,  r.averageSalary(), 0.01);
    }

    @Test
    @DisplayName("mixed ACTIVE + INACTIVE in same dept → both counted in statistics")
    void salaryByDepartment_activeAndInactive_bothCountedInStats() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, 10, "Eng", 90_000, EmployeeStatus.ACTIVE),
                perm(2, 10, "Eng", 50_000, EmployeeStatus.INACTIVE)
        ));
        DepartmentSalaryReport r = analytics.salaryByDepartment().get(10);
        assertEquals(2,      r.headCount());
        assertEquals(70_000, r.averageSalary(), 0.01);
    }

    // ── averageSalaryByRole: multi-employee roles ─────────────────────────────

    @Test
    @DisplayName("averageSalaryByRole — three Engineers with different salaries")
    void averageSalaryByRole_threeEngineers() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, 10, "Engineer", 60_000, EmployeeStatus.ACTIVE),
                perm(2, 10, "Engineer", 80_000, EmployeeStatus.ACTIVE),
                perm(3, 20, "Engineer", 100_000, EmployeeStatus.ACTIVE)
        ));
        double avg = analytics.averageSalaryByRole().get("Engineer");
        assertEquals(80_000, avg, 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — CONTRACT employees included in role average")
    void averageSalaryByRole_contractEmployeesIncluded() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1,     10, "Consultant", 80_000, EmployeeStatus.ACTIVE),
                contract(2, 20, "Consultant", 60_000)
        ));
        assertEquals(70_000, analytics.averageSalaryByRole().get("Consultant"), 0.01);
    }

    // ── partitionByStatus: boundary cases ────────────────────────────────────

    @Test
    @DisplayName("partitionByStatus — all INACTIVE → active bucket empty")
    void partitionByStatus_allInactive_activeBucketEmpty() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, 10, "Eng", 80_000, EmployeeStatus.INACTIVE),
                perm(2, 10, "Eng", 70_000, EmployeeStatus.INACTIVE)
        ));
        Map<Boolean, List<EmployeeSummaryDTO>> result = analytics.partitionByStatus();
        assertTrue(result.get(true).isEmpty());
        assertEquals(2, result.get(false).size());
    }

    @Test
    @DisplayName("partitionByStatus — EmployeeSummaryDTO.from() sets type tag via switch expr")
    void partitionByStatus_dtoTypeTagSetCorrectly() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1,     10, "Eng",  80_000, EmployeeStatus.ACTIVE),
                contract(2, 20, "Cons", 60_000)
        ));
        List<EmployeeSummaryDTO> active = analytics.partitionByStatus().get(true);
        assertEquals(2, active.size());
        long permanents = active.stream().filter(d -> d.employeeType().equals("PERMANENT")).count();
        long contracts  = active.stream().filter(d -> d.employeeType().equals("CONTRACT")).count();
        assertEquals(1, permanents);
        assertEquals(1, contracts);
    }

    // ── topNBySalary: tie-breaking ────────────────────────────────────────────

    @Test
    @DisplayName("topNBySalary(1) — returns the single highest salary employee")
    void topNBySalary_one_returnsHighest() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, 10, "Eng", 50_000, EmployeeStatus.ACTIVE),
                perm(2, 10, "Eng", 90_000, EmployeeStatus.ACTIVE),
                perm(3, 10, "Eng", 70_000, EmployeeStatus.ACTIVE)
        ));
        List<EmployeeSummaryDTO> top = analytics.topNBySalary(1);
        assertEquals(1, top.size());
        assertEquals(90_000, top.get(0).salary(), 0.01);
    }

    @Test
    @DisplayName("topNBySalary — CONTRACT employee can appear in top N")
    void topNBySalary_contractCanBeTop() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1,     10, "Eng",  50_000, EmployeeStatus.ACTIVE),
                contract(2, 20, "Cons", 95_000)
        ));
        List<EmployeeSummaryDTO> top = analytics.topNBySalary(1);
        assertEquals("CONTRACT", top.get(0).employeeType());
        assertEquals(95_000, top.get(0).salary(), 0.01);
    }
}
