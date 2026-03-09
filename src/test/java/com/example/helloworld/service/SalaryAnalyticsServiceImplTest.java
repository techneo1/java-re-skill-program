package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // ── helper ─────────────────────────────────────────��──────────────────────

    /** Builds a minimal ACTIVE PermanentEmployee for ad-hoc parameterised tests. */
    private static PermanentEmployee emp(int id, int deptId, String role, double salary,
                                         EmployeeStatus status) {
        return new PermanentEmployee(id, "Name" + id, "e" + id + "@x.com",
                deptId, role, salary, status, LocalDate.of(2020, 1, 1), true);
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

    @Test
    @DisplayName("groupByDepartment — result map does not contain absent department")
    void groupByDepartment_absentDepartmentIsEmpty() {
        // dept 99 was never added — Optional-style: map lookup returns empty
        Optional<DepartmentSalaryReport> absent =
                Optional.ofNullable(service.groupByDepartment(all).get(99));
        assertTrue(absent.isEmpty(), "dept 99 must not be in the report");
    }

    @Test
    @DisplayName("groupByDepartment — result map contains every expected department (Optional present)")
    void groupByDepartment_expectedDepartmentsArePresent() {
        Map<Integer, DepartmentSalaryReport> result = service.groupByDepartment(all);
        // validate with Optional.ofNullable so the assertion message is meaningful
        for (int deptId : List.of(10, 20, 30)) {
            Optional<DepartmentSalaryReport> opt = Optional.ofNullable(result.get(deptId));
            assertTrue(opt.isPresent(), "dept " + deptId + " must be present");
        }
    }

    @ParameterizedTest(name = "dept={0}  expectedHeadCount={1}  expectedAvg={2}")
    @DisplayName("groupByDepartment — parameterised aggregates per department")
    @CsvSource({
            "10,  2,  87500.0",
            "20,  2,  85000.0",
            "30,  1,  55000.0"
    })
    void groupByDepartment_parameterisedAggregates(int deptId, int expectedHeadCount,
                                                    double expectedAvg) {
        DepartmentSalaryReport report =
                Optional.ofNullable(service.groupByDepartment(all).get(deptId))
                        .orElseThrow(() -> new AssertionError("dept " + deptId + " missing"));
        assertEquals(expectedHeadCount, report.headCount(),     0.01, "headCount");
        assertEquals(expectedAvg,       report.averageSalary(), 0.01, "avg");
    }

    @Test
    @DisplayName("groupByDepartment — single-employee department: min == max == avg")
    void groupByDepartment_singleEmployeeDept_minMaxAvgEqual() {
        // dept 30 has only eve (55_000)
        DepartmentSalaryReport dept30 =
                Optional.ofNullable(service.groupByDepartment(all).get(30))
                        .orElseThrow();
        assertAll(
                () -> assertEquals(55_000, dept30.minSalary(),     0.01),
                () -> assertEquals(55_000, dept30.maxSalary(),     0.01),
                () -> assertEquals(55_000, dept30.averageSalary(), 0.01)
        );
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

    @ParameterizedTest(name = "n={0} → resultSize={1}")
    @DisplayName("topNBySalary — parameterised: result size is min(n, listSize)")
    @CsvSource({
            "1, 1",
            "2, 2",
            "5, 5",
            "6, 5",
            "100, 5"
    })
    void topNBySalary_parameterisedResultSize(int n, int expectedSize) {
        assertEquals(expectedSize, service.topNBySalary(all, n).size());
    }

    @ParameterizedTest(name = "rank={0} → employeeId={1}")
    @DisplayName("topNBySalary — parameterised: descending salary order")
    @CsvSource({
            // dave=110k, bob=90k, alice=85k, carol=60k, eve=55k
            "0, 4",
            "1, 2",
            "2, 1",
            "3, 3",
            "4, 5"
    })
    void topNBySalary_parameterisedOrder(int rank, int expectedId) {
        List<Employee> top5 = service.topNBySalary(all, 5);
        assertEquals(expectedId, top5.get(rank).getId(),
                "rank " + rank + " should be employeeId=" + expectedId);
    }

    @Test
    @DisplayName("topNBySalary — n=1 returns only the single highest earner")
    void topNBySalary_nOne_returnsSingleHighest() {
        List<Employee> result = service.topNBySalary(all, 1);
        assertEquals(1,           result.size());
        assertEquals(dave.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("topNBySalary — all employees have equal salary: returns n employees (stable)")
    void topNBySalary_tiedSalaries_returnsN() {
        List<Employee> tied = List.of(
                emp(10, 10, "Engineer", 80_000, EmployeeStatus.ACTIVE),
                emp(11, 10, "Engineer", 80_000, EmployeeStatus.ACTIVE),
                emp(12, 10, "Engineer", 80_000, EmployeeStatus.ACTIVE)
        );
        List<Employee> result = service.topNBySalary(tied, 2);
        assertEquals(2, result.size(), "should still return exactly 2");
        result.forEach(e -> assertEquals(80_000, e.getSalary(), 0.01));
    }

    @Test
    @DisplayName("topNBySalary — negative n returns empty list")
    void topNBySalary_negativeN_returnsEmpty() {
        assertTrue(service.topNBySalary(all, -1).isEmpty());
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

    @ParameterizedTest(name = "role={0}  expectedAvg={1}")
    @DisplayName("averageSalaryByRole — parameterised expected averages")
    @CsvSource({
            "engineer,   87500.0",
            "designer,   60000.0",
            "manager,   110000.0",
            "qa analyst,  55000.0"
    })
    void averageSalaryByRole_parameterisedAverages(String role, double expectedAvg) {
        double actual = Optional.ofNullable(service.averageSalaryByRole(all).get(role))
                .orElseThrow(() -> new AssertionError("role '" + role + "' not found"));
        assertEquals(expectedAvg, actual, 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — role keys are stripped of whitespace")
    void averageSalaryByRole_whitespaceRoleStripped() {
        List<Employee> withSpaces = List.of(
                emp(20, 10, "  Engineer  ", 80_000, EmployeeStatus.ACTIVE),
                emp(21, 10, "Engineer",     90_000, EmployeeStatus.ACTIVE)
        );
        Map<String, Double> result = service.averageSalaryByRole(withSpaces);
        // both should collapse into the same "engineer" key
        assertEquals(1, result.size(), "whitespace variants should merge into one key");
        assertEquals(85_000, result.get("engineer"), 0.01);
    }

    @Test
    @DisplayName("averageSalaryByRole — absent role returns empty Optional on lookup")
    void averageSalaryByRole_absentRoleIsEmpty() {
        Optional<Double> absent =
                Optional.ofNullable(service.averageSalaryByRole(all).get("ceo"));
        assertTrue(absent.isEmpty(), "'ceo' role must not be present");
    }

    @Test
    @DisplayName("averageSalaryByRole — single employee: avg equals that employee's salary")
    void averageSalaryByRole_singleEmployee_avgEqualsSalary() {
        List<Employee> single = List.of(emp(30, 10, "Architect", 120_000, EmployeeStatus.ACTIVE));
        double avg = Optional.ofNullable(service.averageSalaryByRole(single).get("architect"))
                .orElseThrow();
        assertEquals(120_000, avg, 0.01);
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

    @ParameterizedTest(name = "activeCount={0}  inactiveCount={1}")
    @DisplayName("partitionByStatus — parameterised counts for various mixes")
    @CsvSource({
            "0, 5",   // all inactive
            "3, 2",
            "5, 0"    // all active
    })
    void partitionByStatus_parameterisedCounts(int activeCount, int inactiveCount) {
        List<Employee> mixed = new java.util.ArrayList<>();
        for (int i = 1; i <= activeCount;   i++) mixed.add(emp(i,        10, "Eng", 70_000, EmployeeStatus.ACTIVE));
        for (int i = 1; i <= inactiveCount; i++) mixed.add(emp(100 + i,  10, "Eng", 70_000, EmployeeStatus.INACTIVE));

        Map<Boolean, List<Employee>> result = service.partitionByStatus(mixed);
        assertEquals(activeCount,   result.get(true).size(),  "active count");
        assertEquals(inactiveCount, result.get(false).size(), "inactive count");
    }

    @Test
    @DisplayName("partitionByStatus — all employees inactive: active bucket is empty")
    void partitionByStatus_allInactive_activeBucketEmpty() {
        List<Employee> allInactive = List.of(
                emp(10, 10, "Engineer", 70_000, EmployeeStatus.INACTIVE),
                emp(11, 10, "Engineer", 80_000, EmployeeStatus.INACTIVE)
        );
        Map<Boolean, List<Employee>> result = service.partitionByStatus(allInactive);
        assertTrue(result.get(true).isEmpty(),  "active bucket must be empty");
        assertEquals(2, result.get(false).size(), "inactive bucket must have 2");
    }

    @Test
    @DisplayName("partitionByStatus — active partition list is unmodifiable")
    void partitionByStatus_activeListIsUnmodifiable() {
        Map<Boolean, List<Employee>> result = service.partitionByStatus(all);
        assertThrows(UnsupportedOperationException.class,
                () -> result.get(true).add(alice));
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

    @Test
    @DisplayName("buildReport — byDepartment Optional lookup: known dept present, unknown absent")
    void buildReport_optionalDeptLookup() {
        SalaryAnalyticsReport report = service.buildReport(all);

        Optional<DepartmentSalaryReport> present = Optional.ofNullable(report.byDepartment().get(10));
        Optional<DepartmentSalaryReport> absent  = Optional.ofNullable(report.byDepartment().get(99));

        assertTrue(present.isPresent(), "dept 10 must be present");
        assertTrue(absent.isEmpty(),    "dept 99 must be absent");
    }

    @Test
    @DisplayName("buildReport — avgSalaryByRole Optional lookup: known role present, unknown absent")
    void buildReport_optionalRoleLookup() {
        SalaryAnalyticsReport report = service.buildReport(all);

        Optional<Double> engineer = Optional.ofNullable(report.avgSalaryByRole().get("engineer"));
        Optional<Double> ceo      = Optional.ofNullable(report.avgSalaryByRole().get("ceo"));

        assertTrue(engineer.isPresent(), "engineer must be present");
        assertTrue(ceo.isEmpty(),        "ceo must be absent");
        assertEquals(87_500, engineer.get(), 0.01);
    }

    @Test
    @DisplayName("buildReport — single-employee list produces report with one dept, one role, one active")
    void buildReport_singleEmployee() {
        SalaryAnalyticsReport report = service.buildReport(List.of(alice));

        assertAll(
                () -> assertEquals(1, report.byDepartment().size()),
                () -> assertEquals(1, report.top5BySalary().size()),
                () -> assertEquals(1, report.avgSalaryByRole().size()),
                () -> assertEquals(1, report.activeEmployees().size()),
                () -> assertEquals(0, report.inactiveEmployees().size())
        );
    }

    @Test
    @DisplayName("buildReport — inactive-only list: activeEmployees is empty, inactiveEmployees has all")
    void buildReport_allInactive() {
        List<Employee> inactiveOnly = List.of(
                emp(10, 10, "QA", 50_000, EmployeeStatus.INACTIVE),
                emp(11, 20, "QA", 60_000, EmployeeStatus.INACTIVE)
        );
        SalaryAnalyticsReport report = service.buildReport(inactiveOnly);
        assertTrue(report.activeEmployees().isEmpty(),   "no active employees expected");
        assertEquals(2, report.inactiveEmployees().size(), "both should be inactive");
    }

    @Test
    @DisplayName("buildReport — top5 capped at 5 even when more employees exist")
    void buildReport_top5CappedAtFive() {
        // build 8 employees across various salaries
        List<Employee> large = List.of(
                emp(1, 10, "Eng", 100_000, EmployeeStatus.ACTIVE),
                emp(2, 10, "Eng", 95_000,  EmployeeStatus.ACTIVE),
                emp(3, 10, "Eng", 90_000,  EmployeeStatus.ACTIVE),
                emp(4, 10, "Eng", 85_000,  EmployeeStatus.ACTIVE),
                emp(5, 10, "Eng", 80_000,  EmployeeStatus.ACTIVE),
                emp(6, 10, "Eng", 75_000,  EmployeeStatus.ACTIVE),
                emp(7, 10, "Eng", 70_000,  EmployeeStatus.ACTIVE),
                emp(8, 10, "Eng", 65_000,  EmployeeStatus.ACTIVE)
        );
        SalaryAnalyticsReport report = service.buildReport(large);
        assertEquals(5, report.top5BySalary().size(), "top5 must be capped at 5");
        assertEquals(100_000, report.top5BySalary().get(0).getSalary(), 0.01, "highest first");
    }
}
