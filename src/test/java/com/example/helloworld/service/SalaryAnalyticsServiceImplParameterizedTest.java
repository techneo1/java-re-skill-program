package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Parameterized salary tests for {@link SalaryAnalyticsServiceImpl}.
 *
 * Every test method uses a JUnit 5 parameterized source so the same assertion
 * logic runs across multiple salary configurations without duplicating code:
 *
 * <ul>
 *   <li>{@code @CsvSource}     — inline (input, expected) pairs for averages / totals</li>
 *   <li>{@code @MethodSource}  — complex multi-field employee lists built in a factory</li>
 *   <li>{@code @ValueSource}   — boundary values for {@code topNBySalary(n)}</li>
 *   <li>{@code @EnumSource}    — all {@link EmployeeStatus} variants for partitioning</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAnalyticsServiceImpl — parameterized salary tests")
class SalaryAnalyticsServiceImplParameterizedTest {

    @Mock  private EmployeeService employeeService;
    @InjectMocks private SalaryAnalyticsServiceImpl analytics;

    // ── shared static helpers (used by @MethodSource providers AND tests) ──────

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
    // 1 · averageSalaryByRole — @CsvSource pairs
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Each row is (salary1, salary2, expectedAverage) for two Engineers.
     */
    @ParameterizedTest(name = "Engineer salaries [{0}, {1}] → avg {2}")
    @DisplayName("averageSalaryByRole — correct average for two Engineers")
    @CsvSource({
            "50000,  50000,  50000",
            "60000,  80000,  70000",
            "0,      100000, 50000",
            "75000,  75000,  75000",
            "123456, 234567, 179011.5"
    })
    void averageSalaryByRole_twoEngineers(double s1, double s2, double expected) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Engineer", s1, EmployeeStatus.ACTIVE, 10),
                perm(2, "Engineer", s2, EmployeeStatus.ACTIVE, 20)
        ));
        assertEquals(expected, analytics.averageSalaryByRole().get("Engineer"), 0.01);
    }

    /**
     * Single employee per role — average must equal the employee's salary.
     */
    @ParameterizedTest(name = "single {0} with salary {1} → avg == salary")
    @DisplayName("averageSalaryByRole — single employee per role: average equals salary")
    @CsvSource({
            "Engineer,   85000",
            "Manager,    120000",
            "Analyst,    55000",
            "Consultant, 60000",
            "Director,   200000"
    })
    void averageSalaryByRole_singleEmployeePerRole_avgEqualsSalary(String role, double salary) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, role, salary, EmployeeStatus.ACTIVE, 10)
        ));
        assertEquals(salary, analytics.averageSalaryByRole().get(role), 0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2 · averageSalaryByEmployeeType — @MethodSource
    // ══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> permanentAverageProvider() {
        return Stream.of(
            Arguments.of(
                List.<Employee>of(perm(1, "Eng", 80_000, EmployeeStatus.ACTIVE, 10)),
                80_000.0
            ),
            Arguments.of(
                List.<Employee>of(
                    perm(1, "Eng", 60_000, EmployeeStatus.ACTIVE, 10),
                    perm(2, "Mgr", 90_000, EmployeeStatus.ACTIVE, 10)
                ),
                75_000.0
            ),
            Arguments.of(
                List.<Employee>of(
                    perm(1, "Eng",  60_000, EmployeeStatus.ACTIVE,   10),
                    perm(2, "Mgr",  90_000, EmployeeStatus.ACTIVE,   10),
                    perm(3, "Eng", 120_000, EmployeeStatus.INACTIVE, 20)
                ),
                90_000.0   // (60k + 90k + 120k) / 3
            )
        );
    }

    @ParameterizedTest(name = "permanents {index} → avg {1}")
    @DisplayName("averageSalaryByEmployeeType — PERMANENT average across configurations")
    @MethodSource("permanentAverageProvider")
    void averageSalaryByEmployeeType_permanentAverage(List<Employee> employees,
                                                       double expectedAvg) {
        when(employeeService.getAllEmployees()).thenReturn(employees);
        assertEquals(expectedAvg,
                analytics.averageSalaryByEmployeeType().get("PERMANENT"), 0.01);
    }

    static Stream<Arguments> contractAverageProvider() {
        return Stream.of(
            Arguments.of(
                List.<Employee>of(contract(1, "Cons", 60_000, 10)),
                60_000.0
            ),
            Arguments.of(
                List.<Employee>of(
                    contract(1, "Cons", 40_000, 10),
                    contract(2, "Cons", 80_000, 20)
                ),
                60_000.0
            ),
            Arguments.of(
                List.<Employee>of(
                    contract(1, "Cons",  50_000, 10),
                    contract(2, "Anal",  70_000, 10),
                    contract(3, "Cons", 100_000, 20)
                ),
                73_333.33   // (50k + 70k + 100k) / 3
            )
        );
    }

    @ParameterizedTest(name = "contracts {index} → avg {1}")
    @DisplayName("averageSalaryByEmployeeType — CONTRACT average across configurations")
    @MethodSource("contractAverageProvider")
    void averageSalaryByEmployeeType_contractAverage(List<Employee> employees,
                                                      double expectedAvg) {
        when(employeeService.getAllEmployees()).thenReturn(employees);
        assertEquals(expectedAvg,
                analytics.averageSalaryByEmployeeType().get("CONTRACT"), 0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3 · topNBySalary — @ValueSource boundary values
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "topNBySalary({0}) returns {0} results")
    @DisplayName("topNBySalary — valid n values return correct count")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void topNBySalary_validN_returnsExactN(int n) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 10_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "E", 20_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "E", 30_000, EmployeeStatus.ACTIVE, 10),
                perm(4, "E", 40_000, EmployeeStatus.ACTIVE, 10),
                perm(5, "E", 50_000, EmployeeStatus.ACTIVE, 10)
        ));
        assertEquals(n, analytics.topNBySalary(n).size());
    }

    @ParameterizedTest(name = "topNBySalary({0}) with 3 employees returns 3")
    @DisplayName("topNBySalary — n larger than roster returns all employees")
    @ValueSource(ints = {4, 5, 10, 100, 1_000})
    void topNBySalary_nLargerThanRoster_returnsAll(int n) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "E", 30_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "E", 50_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "E", 40_000, EmployeeStatus.ACTIVE, 10)
        ));
        assertEquals(3, analytics.topNBySalary(n).size());
    }

    @ParameterizedTest(name = "topNBySalary({0}) throws")
    @DisplayName("topNBySalary — non-positive n always throws IllegalArgumentException")
    @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
    void topNBySalary_nonPositiveN_throws(int n) {
        assertThrows(IllegalArgumentException.class, () -> analytics.topNBySalary(n));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4 · salaryByDepartment — @CsvSource (deptId, salary)
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "dept {0} with salary {1}")
    @DisplayName("salaryByDepartment — single employee: total=avg=min=max=salary")
    @CsvSource({
            "1,  50000",
            "5,  75000",
            "10, 85000",
            "99, 200000"
    })
    void salaryByDepartment_singleEmployee_statsAreAllSalary(int deptId, double salary) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng", salary, EmployeeStatus.ACTIVE, deptId)
        ));
        DepartmentSalaryReport r = analytics.salaryByDepartment().get(deptId);
        assertNotNull(r, "report must exist for dept " + deptId);
        assertEquals(1,      r.headCount());
        assertEquals(salary, r.totalSalary(),   0.01);
        assertEquals(salary, r.averageSalary(),  0.01);
        assertEquals(salary, r.minSalary(),      0.01);
        assertEquals(salary, r.maxSalary(),      0.01);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5 · partitionByStatus — @EnumSource
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "single {0} employee → correct bucket")
    @DisplayName("partitionByStatus — single employee lands in correct bucket for each status")
    @EnumSource(EmployeeStatus.class)
    void partitionByStatus_singleEmployee_landsInCorrectBucket(EmployeeStatus status) {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                perm(1, "Eng", 80_000, status, 10)
        ));
        Map<Boolean, List<EmployeeSummaryDTO>> result = analytics.partitionByStatus();
        boolean isActive = status == EmployeeStatus.ACTIVE;
        assertEquals(1, result.get(isActive).size());
        assertEquals(0, result.get(!isActive).size());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6 · top-1 always equals highest salary — @MethodSource
    // ══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> highestSalaryProvider() {
        return Stream.of(
            Arguments.of(List.<Employee>of(
                perm(1, "E", 10_000, EmployeeStatus.ACTIVE, 10),
                perm(2, "E", 50_000, EmployeeStatus.ACTIVE, 10),
                perm(3, "E", 30_000, EmployeeStatus.ACTIVE, 10)
            ), 50_000.0),
            Arguments.of(List.<Employee>of(
                contract(1, "C", 200_000, 10),
                perm(2, "E",  80_000, EmployeeStatus.ACTIVE, 10)
            ), 200_000.0),
            Arguments.of(List.<Employee>of(
                perm(1, "E", 99_999, EmployeeStatus.INACTIVE, 10),
                perm(2, "E", 99_998, EmployeeStatus.ACTIVE,   10)
            ), 99_999.0)
        );
    }

    @ParameterizedTest(name = "top-1 salary is {1}")
    @DisplayName("topNBySalary(1) — always returns the single highest salary")
    @MethodSource("highestSalaryProvider")
    void topNBySalary_one_alwaysReturnsHighest(List<Employee> employees, double expectedTop) {
        when(employeeService.getAllEmployees()).thenReturn(employees);
        List<EmployeeSummaryDTO> top = analytics.topNBySalary(1);
        assertEquals(1, top.size());
        assertEquals(expectedTop, top.get(0).salary(), 0.01);
    }
}
