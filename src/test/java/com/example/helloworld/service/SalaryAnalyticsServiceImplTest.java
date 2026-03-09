package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SalaryAnalyticsServiceImpl")
class SalaryAnalyticsServiceImplTest {

    private SalaryAnalyticsServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────
    // dept 10: alice (Engineer, 85_000 ACTIVE), bob (Engineer, 90_000 ACTIVE)
    // dept 20: carol (Designer, 60_000 ACTIVE), dave (Manager, 110_000 ACTIVE)
    // dept 30: eve   (QA Analyst, 55_000 INACTIVE)

    private PermanentEmployee alice;
    private PermanentEmployee bob;
    private PermanentEmployee carol;
    private PermanentEmployee dave;
    private PermanentEmployee eve;
    private List<Employee>    all;

    @BeforeEach
    void setUp() {
        service = new SalaryAnalyticsServiceImpl();

        alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);

        bob = new PermanentEmployee(2, "Bob Singh", "bob@example.com",
                10, "Engineer", 90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), true);

        carol = new PermanentEmployee(3, "Carol Menon", "carol@example.com",
                20, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), false);

        dave = new PermanentEmployee(4, "Dave Patel", "dave@example.com",
                20, "Manager", 110_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2017, 8, 20), true);

        eve = new PermanentEmployee(5, "Eve Sharma", "eve@example.com",
                30, "QA Analyst", 55_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2022, 5, 10), false);

        all = List.of(alice, bob, carol, dave, eve);
    }

    // ── groupByDepartment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("groupByDepartment — correct number of departments")
    void groupByDepartment_correctDepartmentCount() {
        Map<Integer, DepartmentSalaryReport> result = service.groupByDepartment(all);
        assertEquals(3, result.size(), "should have 3 departments");
    }

    @Test
    @DisplayName("groupByDepartment — dept 10: headCount=2, avg=87_500")
    void groupByDepartment_dept10Aggregates() {
        DepartmentSalaryReport dept10 = service.groupByDepartment(all).get(10);
        assertNotNull(dept10, "dept 10 report must not be null");
        assertEquals(2,       dept10.headCount(),     "headCount");
        assertEquals(85_000,  dept10.minSalary(),     0.01, "min");
        assertEquals(90_000,  dept10.maxSalary(),     0.01, "max");
        assertEquals(87_500,  dept10.averageSalary(), 0.01, "avg");
        assertEquals(175_000, dept10.totalSalary(),   0.01, "total");
    }

    @Test
    @DisplayName("groupByDepartment — dept 20: headCount=2, avg=85_000")
    void groupByDepartment_dept20Aggregates() {
        DepartmentSalaryReport dept20 = service.groupByDepartment(all).get(20);
        assertNotNull(dept20);
        assertEquals(2,       dept20.headCount(),     "headCount");
        assertEquals(60_000,  dept20.minSalary(),     0.01, "min");
        assertEquals(110_000, dept20.maxSalary(),     0.01, "max");
        assertEquals(85_000,  dept20.averageSalary(), 0.01, "avg");
    }

    @Test
    @DisplayName("groupByDepartment — empty input returns empty map")
    void groupByDepartment_emptyInput() {
        assertTrue(service.groupByDepartment(Collections.emptyList()).isEmpty());
    }

    // ── topNBySalary ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("topNBySalary — top 3 sorted descending")
    void topNBySalary_top3() {
        List<Employee> top3 = service.topNBySalary(all, 3);
        assertEquals(3, top3.size());
        // dave (110_000) > bob (90_000) > alice (85_000)
        assertEquals(dave.getId(),  top3.get(0).getId(), "1st: dave");
        assertEquals(bob.getId(),   top3.get(1).getId(), "2nd: bob");
        assertEquals(alice.getId(), top3.get(2).getId(), "3rd: alice");
    }

    @Test
    @DisplayName("topNBySalary — n > list size returns full list")
    void topNBySalary_nLargerThanList() {
        List<Employee> result = service.topNBySalary(all, 100);
        assertEquals(all.size(), result.size());
    }

    @Test
    @DisplayName("topNBySalary — n=0 returns empty list")
    void topNBySalary_nZero() {
        assertTrue(service.topNBySalary(all, 0).isEmpty());
    }

    @Test
    @DisplayName("topNBySalary — empty input returns empty list")
    void topNBySalary_emptyInput() {
        assertTrue(service.topNBySalary(Collections.emptyList(), 5).isEmpty());
    }

    // ── averageSalaryByRole ───────────────────────────────────────────────────

    @Test
    @DisplayName("averageSalaryByRole — engineer avg = 87_500")
    void averageSalaryByRole_engineerAvg() {
        Map<String, Double> result = service.averageSalaryByRole(all);
        assertTrue(result.containsKey("engineer"), "key should be lower-cased");
        assertEquals(87_500, result.get("engineer"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — roles are lower-cased")
    void averageSalaryByRole_keysAreLowerCased() {
        Map<String, Double> result = service.averageSalaryByRole(all);
        result.keySet().forEach(role ->
                assertEquals(role, role.toLowerCase(), "role key must be lower-case"));
    }

    @Test
    @DisplayName("averageSalaryByRole — distinct roles count")
    void averageSalaryByRole_distinctRoleCount() {
        // engineer, designer, manager, qa analyst
        assertEquals(4, service.averageSalaryByRole(all).size());
    }

    @Test
    @DisplayName("averageSalaryByRole — empty input returns empty map")
    void averageSalaryByRole_emptyInput() {
        assertTrue(service.averageSalaryByRole(Collections.emptyList()).isEmpty());
    }

    // ── partitionByStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("partitionByStatus — 4 active employees")
    void partitionByStatus_activeCount() {
        Map<Boolean, List<Employee>> result = service.partitionByStatus(all);
        assertEquals(4, result.get(true).size(), "active count");
    }

    @Test
    @DisplayName("partitionByStatus — 1 inactive employee")
    void partitionByStatus_inactiveCount() {
        Map<Boolean, List<Employee>> result = service.partitionByStatus(all);
        assertEquals(1, result.get(false).size(), "inactive count");
        assertEquals(eve.getId(), result.get(false).get(0).getId());
    }

    @Test
    @DisplayName("partitionByStatus — both keys present even when one bucket is empty")
    void partitionByStatus_bothKeysAlwaysPresent() {
        List<Employee> activeOnly = List.of(alice, bob);
        Map<Boolean, List<Employee>> result = service.partitionByStatus(activeOnly);
        assertTrue(result.containsKey(true),  "true key must be present");
        assertTrue(result.containsKey(false), "false key must be present");
        assertTrue(result.get(false).isEmpty(), "inactive bucket should be empty");
    }

    @Test
    @DisplayName("partitionByStatus — empty input returns empty partitions")
    void partitionByStatus_emptyInput() {
        Map<Boolean, List<Employee>> result = service.partitionByStatus(Collections.emptyList());
        assertTrue(result.get(true).isEmpty());
        assertTrue(result.get(false).isEmpty());
    }

    // ── buildReport ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildReport — report is fully populated")
    void buildReport_fullyPopulated() {
        SalaryAnalyticsReport report = service.buildReport(all);

        assertNotNull(report);
        assertEquals(3, report.byDepartment().size(),      "departments");
        assertEquals(5, report.top5BySalary().size(),      "top-5 (all 5 employees)");
        assertEquals(4, report.avgSalaryByRole().size(),   "roles");
        assertEquals(4, report.activeEmployees().size(),   "active");
        assertEquals(1, report.inactiveEmployees().size(), "inactive");
    }

    @Test
    @DisplayName("buildReport — top5 first element is highest paid")
    void buildReport_top5FirstIsHighestPaid() {
        SalaryAnalyticsReport report = service.buildReport(all);
        assertEquals(dave.getId(), report.top5BySalary().get(0).getId(),
                "dave (110_000) should lead");
    }

    @Test
    @DisplayName("buildReport — empty input produces empty report")
    void buildReport_emptyInput() {
        SalaryAnalyticsReport report = service.buildReport(Collections.emptyList());
        assertNotNull(report);
        assertTrue(report.byDepartment().isEmpty());
        assertTrue(report.top5BySalary().isEmpty());
        assertTrue(report.avgSalaryByRole().isEmpty());
        assertTrue(report.activeEmployees().isEmpty());
        assertTrue(report.inactiveEmployees().isEmpty());
    }
}

