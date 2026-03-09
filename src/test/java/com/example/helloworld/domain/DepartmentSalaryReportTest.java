package com.example.helloworld.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DepartmentSalaryReport")
class DepartmentSalaryReportTest {

    private static final LocalDate JOINING = LocalDate.of(2020, 1, 1);

    private static PermanentEmployee emp(int id, String email, double salary) {
        return new PermanentEmployee(id, "Name" + id, email, 10, "Engineer",
                salary, EmployeeStatus.ACTIVE, JOINING, true);
    }

    // ── Record structural guarantees ──────────────────────────────────────────

    @Test
    @DisplayName("of — headCount equals the number of employees supplied")
    void of_headCount() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 80_000), emp(2, "b@x.com", 90_000)));

        assertEquals(2, report.headCount());
    }

    @Test
    @DisplayName("of — totalSalary is the sum of all salaries")
    void of_totalSalary() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 80_000), emp(2, "b@x.com", 90_000)));

        assertEquals(170_000, report.totalSalary(), 0.01);
    }

    @Test
    @DisplayName("of — averageSalary is computed correctly")
    void of_averageSalary() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 70_000), emp(2, "b@x.com", 90_000)));

        assertEquals(80_000, report.averageSalary(), 0.01);
    }

    @Test
    @DisplayName("of — minSalary is the lowest salary in the group")
    void of_minSalary() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 50_000), emp(2, "b@x.com", 90_000),
                        emp(3, "c@x.com", 70_000)));

        assertEquals(50_000, report.minSalary(), 0.01);
    }

    @Test
    @DisplayName("of — maxSalary is the highest salary in the group")
    void of_maxSalary() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 50_000), emp(2, "b@x.com", 90_000),
                        emp(3, "c@x.com", 70_000)));

        assertEquals(90_000, report.maxSalary(), 0.01);
    }

    @Test
    @DisplayName("of — single employee: min == max == avg == salary")
    void of_singleEmployee_minMaxAvgEqual() {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", 75_000)));

        assertAll(
                () -> assertEquals(1,      report.headCount()),
                () -> assertEquals(75_000, report.minSalary(),     0.01),
                () -> assertEquals(75_000, report.maxSalary(),     0.01),
                () -> assertEquals(75_000, report.averageSalary(), 0.01),
                () -> assertEquals(75_000, report.totalSalary(),   0.01)
        );
    }

    // ── Parameterised: aggregates across combinations ─────────────────────────

    @ParameterizedTest(name = "salaries [{0}, {1}] → avg={2}, min={3}, max={4}")
    @DisplayName("of — correct aggregates for salary pairs (parameterised)")
    @CsvSource({
            "100000, 0,      50000,  0,      100000",
            "50000,  50000,  50000,  50000,  50000",
            "30000,  90000,  60000,  30000,  90000",
            "1,      99999,  50000,  1,      99999"
    })
    void of_aggregates_parameterised(double s1, double s2,
                                      double expAvg, double expMin, double expMax) {
        var report = DepartmentSalaryReport.of(10,
                List.of(emp(1, "a@x.com", s1), emp(2, "b@x.com", s2)));

        assertAll(
                () -> assertEquals(expAvg, report.averageSalary(), 0.01),
                () -> assertEquals(expMin, report.minSalary(),     0.01),
                () -> assertEquals(expMax, report.maxSalary(),     0.01)
        );
    }

    // ── Record equality ───────────────────────────────────────────────────────

    @Test
    @DisplayName("two reports built from identical data are equal (Record equals)")
    void recordEquals_identicalData() {
        List<Employee> employees = List.of(emp(1, "a@x.com", 80_000), emp(2, "b@x.com", 90_000));

        assertEquals(DepartmentSalaryReport.of(10, employees),
                     DepartmentSalaryReport.of(10, employees));
    }

    @Test
    @DisplayName("reports for different departments are not equal")
    void recordEquals_differentDepartments() {
        List<Employee> employees = List.of(emp(1, "a@x.com", 80_000));

        assertNotEquals(DepartmentSalaryReport.of(10, employees),
                        DepartmentSalaryReport.of(20, employees));
    }

    // ── Compact constructor guards ────────────────────────────────────────────

    @Test
    @DisplayName("of — throws IllegalArgumentException for empty employee list")
    void of_emptyList_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> DepartmentSalaryReport.of(10, List.of()));
    }

    @Test
    @DisplayName("of — throws NullPointerException for null employee list")
    void of_nullList_throws() {
        assertThrows(NullPointerException.class,
                () -> DepartmentSalaryReport.of(10, null));
    }

    @Test
    @DisplayName("compact constructor — throws for departmentId <= 0")
    void constructor_invalidDepartmentId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(0, 1, 50_000, 50_000, 50_000, 50_000));
    }
}
