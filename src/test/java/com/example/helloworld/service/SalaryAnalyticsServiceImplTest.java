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
 * Unit tests for {@link SalaryAnalyticsServiceImpl}.
 *
 * Uses Mockito to stub {@link EmployeeService#getAllEmployees()} so every
 * test is independent of any repository or DB.
 *
 * Covers:
 * - salaryByDepartment()    — groupingBy + summarizingDouble
 * - top5BySalary()          — sorted + limit + EmployeeSummaryDTO.from()
 * - averageSalaryByRole()   — groupingBy + averagingDouble
 * - partitionByStatus()     — partitioningBy → true/false buckets
 * - averageSalaryByEmployeeType() — switch expression over sealed hierarchy
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAnalyticsServiceImpl")
class SalaryAnalyticsServiceImplTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private SalaryAnalyticsServiceImpl analytics;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** dept=10, role=Engineer,   salary=85_000, ACTIVE,   PERMANENT */
    private static PermanentEmployee alice() {
        return new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    /** dept=10, role=Manager,    salary=120_000, ACTIVE,  PERMANENT */
    private static PermanentEmployee bob() {
        return new PermanentEmployee(2, "Bob Singh", "bob@example.com",
                10, "Manager", 120_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), false);
    }

    /** dept=20, role=Consultant, salary=60_000,  ACTIVE,  CONTRACT */
    private static ContractEmployee carol() {
        return new ContractEmployee(3, "Carol White", "carol@example.com",
                20, "Consultant", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));
    }

    /** dept=20, role=Engineer,   salary=70_000,  INACTIVE, PERMANENT */
    private static PermanentEmployee dave() {
        return new PermanentEmployee(4, "Dave Rao", "dave@example.com",
                20, "Engineer", 70_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2018, 4, 1), false);
    }

    /** dept=30, role=Analyst,    salary=55_000,  ACTIVE,  CONTRACT */
    private static ContractEmployee eve() {
        return new ContractEmployee(5, "Eve Chen", "eve@example.com",
                30, "Analyst", 55_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 7, 1), LocalDate.of(2026, 6, 30));
    }

    private List<Employee> allFive() {
        return List.of(alice(), bob(), carol(), dave(), eve());
    }

    // ── salaryByDepartment() ──────────────────────────────────────────────────

    @Test
    @DisplayName("salaryByDepartment — correct departmentIds as keys")
    void salaryByDepartment_keysAreCorrectDeptIds() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        Map<Integer, DepartmentSalaryReport> result = analytics.salaryByDepartment();
        assertTrue(result.containsKey(10));
        assertTrue(result.containsKey(20));
        assertTrue(result.containsKey(30));
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("salaryByDepartment — headCount is correct per department")
    void salaryByDepartment_headCountCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        Map<Integer, DepartmentSalaryReport> result = analytics.salaryByDepartment();
        assertEquals(2, result.get(10).headCount());   // alice + bob
        assertEquals(2, result.get(20).headCount());   // carol + dave
        assertEquals(1, result.get(30).headCount());   // eve
    }

    @Test
    @DisplayName("salaryByDepartment — totalSalary correct for dept 10")
    void salaryByDepartment_totalSalaryCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        DepartmentSalaryReport dept10 = analytics.salaryByDepartment().get(10);
        assertEquals(205_000, dept10.totalSalary(), 0.01);  // 85k + 120k
    }

    @Test
    @DisplayName("salaryByDepartment — averageSalary correct for dept 10")
    void salaryByDepartment_averageSalaryCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        DepartmentSalaryReport dept10 = analytics.salaryByDepartment().get(10);
        assertEquals(102_500, dept10.averageSalary(), 0.01);
    }

    @Test
    @DisplayName("salaryByDepartment — minSalary and maxSalary correct for dept 10")
    void salaryByDepartment_minMaxCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        DepartmentSalaryReport dept10 = analytics.salaryByDepartment().get(10);
        assertEquals(85_000,  dept10.minSalary(), 0.01);
        assertEquals(120_000, dept10.maxSalary(), 0.01);
    }

    @Test
    @DisplayName("salaryByDepartment — result map is sorted by departmentId ascending")
    void salaryByDepartment_sortedByDeptId() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<Integer> keys = List.copyOf(analytics.salaryByDepartment().keySet());
        assertEquals(List.of(10, 20, 30), keys);
    }

    @Test
    @DisplayName("salaryByDepartment — empty employee list returns empty map")
    void salaryByDepartment_empty_returnsEmptyMap() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());
        assertTrue(analytics.salaryByDepartment().isEmpty());
    }

    @Test
    @DisplayName("salaryByDepartment — single-employee dept: min == max == salary")
    void salaryByDepartment_singleEmployee_minEqualsMax() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(eve()));
        DepartmentSalaryReport dept30 = analytics.salaryByDepartment().get(30);
        assertEquals(55_000, dept30.minSalary(), 0.01);
        assertEquals(55_000, dept30.maxSalary(), 0.01);
    }

    // ── topNBySalary() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("top5BySalary — returns employees sorted by salary descending")
    void top5BySalary_sortedDescending() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<EmployeeSummaryDTO> top = analytics.top5BySalary();
        assertEquals(5, top.size());
        assertEquals(120_000, top.get(0).salary(), 0.01);  // bob
        assertEquals(85_000,  top.get(1).salary(), 0.01);  // alice
        assertEquals(70_000,  top.get(2).salary(), 0.01);  // dave
        assertEquals(60_000,  top.get(3).salary(), 0.01);  // carol
        assertEquals(55_000,  top.get(4).salary(), 0.01);  // eve
    }

    @Test
    @DisplayName("topNBySalary(3) — returns only top 3")
    void topNBySalary_limitRespected() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<EmployeeSummaryDTO> top3 = analytics.topNBySalary(3);
        assertEquals(3, top3.size());
        assertEquals(120_000, top3.get(0).salary(), 0.01);
    }

    @Test
    @DisplayName("topNBySalary — employeeType tag set correctly via switch expression")
    void topNBySalary_employeeTypeTagCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<EmployeeSummaryDTO> top = analytics.top5BySalary();
        // bob is PERMANENT (highest salary)
        assertEquals("PERMANENT", top.get(0).employeeType());
        // carol is CONTRACT (4th)
        assertEquals("CONTRACT", top.get(3).employeeType());
    }

    @Test
    @DisplayName("topNBySalary — when fewer employees than N exist, returns all")
    void topNBySalary_fewerThanN_returnsAll() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(alice(), bob()));
        List<EmployeeSummaryDTO> top = analytics.topNBySalary(10);
        assertEquals(2, top.size());
    }

    @Test
    @DisplayName("topNBySalary — empty list returns empty result")
    void topNBySalary_empty_returnsEmpty() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());
        assertTrue(analytics.top5BySalary().isEmpty());
    }

    @Test
    @DisplayName("topNBySalary — n <= 0 throws IllegalArgumentException")
    void topNBySalary_invalidN_throws() {
        assertThrows(IllegalArgumentException.class, () -> analytics.topNBySalary(0));
        assertThrows(IllegalArgumentException.class, () -> analytics.topNBySalary(-1));
    }

    // ── averageSalaryByRole() ─────────────────────────────────────────────────

    @Test
    @DisplayName("averageSalaryByRole — correct keys (all distinct roles)")
    void averageSalaryByRole_correctKeys() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        Map<String, Double> result = analytics.averageSalaryByRole();
        assertTrue(result.containsKey("Engineer"));
        assertTrue(result.containsKey("Manager"));
        assertTrue(result.containsKey("Consultant"));
        assertTrue(result.containsKey("Analyst"));
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("averageSalaryByRole — Engineer average is correct (alice + dave)")
    void averageSalaryByRole_engineerAverageCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        // alice 85k (dept 10) + dave 70k (dept 20) → avg = 77_500
        assertEquals(77_500, analytics.averageSalaryByRole().get("Engineer"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — single-employee role returns that salary as average")
    void averageSalaryByRole_singleEmployeeRole_returnsThatSalary() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        assertEquals(120_000, analytics.averageSalaryByRole().get("Manager"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — map is sorted by role name ascending")
    void averageSalaryByRole_sortedByRoleName() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<String> keys = List.copyOf(analytics.averageSalaryByRole().keySet());
        // Analyst, Consultant, Engineer, Manager
        assertEquals(List.of("Analyst", "Consultant", "Engineer", "Manager"), keys);
    }

    @Test
    @DisplayName("averageSalaryByRole — empty list returns empty map")
    void averageSalaryByRole_empty_returnsEmptyMap() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());
        assertTrue(analytics.averageSalaryByRole().isEmpty());
    }

    // ── partitionByStatus() ───────────────────────────────────────────────────

    @Test
    @DisplayName("partitionByStatus — map always has exactly two keys: true and false")
    void partitionByStatus_alwaysHasTwoKeys() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        Map<Boolean, List<EmployeeSummaryDTO>> result = analytics.partitionByStatus();
        assertTrue(result.containsKey(true));
        assertTrue(result.containsKey(false));
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("partitionByStatus — active bucket contains only ACTIVE employees")
    void partitionByStatus_activeBucketCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<EmployeeSummaryDTO> active = analytics.partitionByStatus().get(true);
        assertEquals(4, active.size());  // alice, bob, carol, eve
        assertTrue(active.stream().allMatch(e -> e.status() == EmployeeStatus.ACTIVE));
    }

    @Test
    @DisplayName("partitionByStatus — inactive bucket contains only INACTIVE employees")
    void partitionByStatus_inactiveBucketCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        List<EmployeeSummaryDTO> inactive = analytics.partitionByStatus().get(false);
        assertEquals(1, inactive.size());  // dave only
        assertEquals("Dave Rao", inactive.get(0).name());
        assertEquals(EmployeeStatus.INACTIVE, inactive.get(0).status());
    }

    @Test
    @DisplayName("partitionByStatus — all active → inactive bucket is empty")
    void partitionByStatus_allActive_inactiveBucketEmpty() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(alice(), bob()));
        assertTrue(analytics.partitionByStatus().get(false).isEmpty());
    }

    @Test
    @DisplayName("partitionByStatus — empty list gives both buckets empty")
    void partitionByStatus_empty_bothBucketsEmpty() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());
        Map<Boolean, List<EmployeeSummaryDTO>> result = analytics.partitionByStatus();
        assertTrue(result.get(true).isEmpty());
        assertTrue(result.get(false).isEmpty());
    }

    // ── averageSalaryByEmployeeType() ─────────────────────────────────────────

    @Test
    @DisplayName("averageSalaryByEmployeeType — keys are PERMANENT and CONTRACT")
    void averageSalaryByEmployeeType_correctKeys() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        Map<String, Double> result = analytics.averageSalaryByEmployeeType();
        assertTrue(result.containsKey("PERMANENT"));
        assertTrue(result.containsKey("CONTRACT"));
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("averageSalaryByEmployeeType — PERMANENT average correct (alice+bob+dave)")
    void averageSalaryByEmployeeType_permanentAverageCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        // alice 85k + bob 120k + dave 70k → avg = 91_666.67
        double expected = (85_000 + 120_000 + 70_000) / 3.0;
        assertEquals(expected, analytics.averageSalaryByEmployeeType().get("PERMANENT"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByEmployeeType — CONTRACT average correct (carol+eve)")
    void averageSalaryByEmployeeType_contractAverageCorrect() {
        when(employeeService.getAllEmployees()).thenReturn(allFive());
        // carol 60k + eve 55k → avg = 57_500
        assertEquals(57_500, analytics.averageSalaryByEmployeeType().get("CONTRACT"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByEmployeeType — only PERMANENT employees → no CONTRACT key")
    void averageSalaryByEmployeeType_onlyPermanent_noContractKey() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(alice(), bob()));
        Map<String, Double> result = analytics.averageSalaryByEmployeeType();
        assertTrue(result.containsKey("PERMANENT"));
        assertFalse(result.containsKey("CONTRACT"));
    }

    @Test
    @DisplayName("averageSalaryByEmployeeType — empty list returns empty map")
    void averageSalaryByEmployeeType_empty_returnsEmptyMap() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());
        assertTrue(analytics.averageSalaryByEmployeeType().isEmpty());
    }

    // ── Constructor guard ─────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor throws NullPointerException when EmployeeService is null")
    void constructor_nullService_throws() {
        assertThrows(NullPointerException.class,
                () -> new SalaryAnalyticsServiceImpl(null));
    }
}

