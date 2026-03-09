package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SalaryAnalyticsServiceImpl.groupByRole() and
 * the enhanced Comparator-chaining behaviour in topNBySalary().
 */
@DisplayName("SalaryAnalyticsServiceImpl — groupByRole + Comparator chaining")
class SalaryAnalyticsServiceImplGroupByRoleTest {

    private SalaryAnalyticsServiceImpl service;

    // dept 10: alice (Engineer, 85_000 ACTIVE), bob (Engineer, 90_000 ACTIVE)
    // dept 20: carol (Designer, 60_000 ACTIVE), dave (Manager, 110_000 ACTIVE)
    // dept 30: eve   (QA Analyst, 55_000 INACTIVE)
    // dept 10: frank (Engineer, 90_000 ACTIVE) — same salary as bob, later name

    private PermanentEmployee alice;
    private PermanentEmployee bob;
    private PermanentEmployee carol;
    private PermanentEmployee dave;
    private PermanentEmployee eve;
    private PermanentEmployee frank;
    private List<Employee>    all;

    @BeforeEach
    void setUp() {
        service = new SalaryAnalyticsServiceImpl();

        alice = new PermanentEmployee(1, "Alice Kumar",  "alice@example.com",
                10, "Engineer",   85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);

        bob = new PermanentEmployee(2, "Bob Singh",    "bob@example.com",
                10, "Engineer",   90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), true);

        carol = new PermanentEmployee(3, "Carol Menon", "carol@example.com",
                20, "Designer",   60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), false);

        dave = new PermanentEmployee(4, "Dave Patel",  "dave@example.com",
                20, "Manager",    110_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2017, 8, 20), true);

        eve = new PermanentEmployee(5, "Eve Sharma",  "eve@example.com",
                30, "QA Analyst", 55_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2022, 5, 10), false);

        // same salary as bob — used to verify tiebreaker ordering
        frank = new PermanentEmployee(6, "Frank Rao",   "frank@example.com",
                10, "Engineer",   90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2021, 2, 1), true);

        all = List.of(alice, bob, carol, dave, eve, frank);
    }

    // ── groupByRole — basic structure ─────────────────────────────────────────

    @Test
    @DisplayName("groupByRole — correct number of distinct roles")
    void groupByRole_correctRoleCount() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        // "engineer", "designer", "manager", "qa analyst"
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("groupByRole — engineer bucket contains 3 employees")
    void groupByRole_engineerBucketSize() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        List<Employee> engineers = result.get("engineer");
        assertNotNull(engineers, "engineer bucket must exist");
        assertEquals(3, engineers.size());
    }

    @Test
    @DisplayName("groupByRole — each bucket key is lower-cased")
    void groupByRole_keysAreLowerCased() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        result.keySet().forEach(key ->
                assertEquals(key, key.toLowerCase(), "key must be lower-cased: " + key));
    }

    @Test
    @DisplayName("groupByRole — empty input returns empty map")
    void groupByRole_emptyInput() {
        assertTrue(service.groupByRole(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("groupByRole — single-employee role has one element")
    void groupByRole_singleEmployeeRole() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        assertEquals(1, result.get("designer").size());
        assertEquals(carol, result.get("designer").get(0));
    }

    // ── groupByRole — ordering within bucket (Comparator chaining) ───────────

    @Test
    @DisplayName("groupByRole — engineer bucket sorted: salary desc, then name asc")
    void groupByRole_engineerBucketOrder() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        List<Employee> engineers = result.get("engineer");

        // salary 90_000: bob (B) comes before frank (F) alphabetically
        assertEquals(bob.getId(),   engineers.get(0).getId(), "first should be Bob (90k, B)");
        assertEquals(frank.getId(), engineers.get(1).getId(), "second should be Frank (90k, F)");
        assertEquals(alice.getId(), engineers.get(2).getId(), "third should be Alice (85k)");
    }

    @Test
    @DisplayName("groupByRole — result map is unmodifiable")
    void groupByRole_resultIsUnmodifiable() {
        Map<String, List<Employee>> result = service.groupByRole(all);
        assertThrows(UnsupportedOperationException.class,
                () -> result.put("new-role", List.of()));
    }

    @Test
    @DisplayName("groupByRole — inner lists are unmodifiable")
    void groupByRole_innerListsAreUnmodifiable() {
        List<Employee> engineers = service.groupByRole(all).get("engineer");
        assertThrows(UnsupportedOperationException.class,
                () -> engineers.add(alice));
    }

    // ── Parameterised: every role produces correct head-count ─────────────────

    @ParameterizedTest(name = "role={0}  expectedSize={1}")
    @DisplayName("groupByRole — parameterised role head-counts")
    @CsvSource({
            "engineer,  3",
            "designer,  1",
            "manager,   1",
            "qa analyst, 1"
    })
    void groupByRole_parameterisedHeadCounts(String role, int expectedSize) {
        Map<String, List<Employee>> result = service.groupByRole(all);
        List<Employee> bucket = result.get(role);
        assertNotNull(bucket, "bucket for role '" + role + "' must exist");
        assertEquals(expectedSize, bucket.size());
    }

    // ── Comparator chaining in topNBySalary ───────────────────────────────────

    @Test
    @DisplayName("topNBySalary — tiebreaker: equal salary ordered by name asc")
    void topNBySalary_tiebreakerByName() {
        // bob and frank both have 90_000
        List<Employee> top3 = service.topNBySalary(all, 3);
        // dave=110k first, then bob (B) before frank (F) at 90k
        assertEquals(dave.getId(),  top3.get(0).getId(), "1st: dave 110k");
        assertEquals(bob.getId(),   top3.get(1).getId(), "2nd: bob 90k (B < F)");
        assertEquals(frank.getId(), top3.get(2).getId(), "3rd: frank 90k");
    }

    @Test
    @DisplayName("topNBySalary — result is unmodifiable")
    void topNBySalary_resultIsUnmodifiable() {
        List<Employee> top5 = service.topNBySalary(all, 5);
        assertThrows(UnsupportedOperationException.class, () -> top5.add(alice));
    }

    @Test
    @DisplayName("topNBySalary — n=0 returns empty list")
    void topNBySalary_zeroN_returnsEmpty() {
        assertTrue(service.topNBySalary(all, 0).isEmpty());
    }

    @Test
    @DisplayName("topNBySalary — n greater than list size returns all sorted")
    void topNBySalary_nExceedsSize_returnsAll() {
        List<Employee> result = service.topNBySalary(all, 100);
        assertEquals(all.size(), result.size());
    }

    // ── buildReport includes groupByRole ─────────────────────────────────────

    @Test
    @DisplayName("buildReport — byRole is populated in the report")
    void buildReport_byRoleIsPopulated() {
        var report = service.buildReport(all);
        assertNotNull(report.byRole(), "byRole must not be null");
        assertFalse(report.byRole().isEmpty(), "byRole must not be empty");
        assertEquals(4, report.byRole().size(), "4 distinct roles");
    }

    @Test
    @DisplayName("buildReport — top5BySalary respects Comparator chain")
    void buildReport_top5UsesComparatorChain() {
        var report = service.buildReport(all);
        List<Employee> top5 = report.top5BySalary();
        // dave (110k) must be first
        assertEquals(dave.getId(), top5.get(0).getId());
    }
}

