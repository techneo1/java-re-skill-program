package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.PayrollException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PayrollServiceImpl")
class PayrollServiceImplTest {

    private PayrollServiceImpl payrollService;
    private static final LocalDate PAYROLL_MONTH = LocalDate.of(2026, 3, 1);

    // ── Lifecycle hooks ───────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        payrollService = new PayrollServiceImpl();
    }

    // ── PermanentEmployee — 20% tax ───────────────────────────────────────

    @Test
    @DisplayName("processPayroll — PermanentEmployee applies 20% tax rate")
    void processPayroll_permanent_twentyPercentTax() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "Alice", "alice@example.com",
                10, "Engineer", 100_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = payrollService.processPayroll(1, emp, PAYROLL_MONTH);
        assertEquals(100_000.0, record.grossSalary());
        assertEquals(20_000.0,  record.taxAmount());
        assertEquals(80_000.0,  record.netSalary());
        assertEquals(emp.getId(), record.employeeId());
    }

    @ParameterizedTest(name = "permanent salary={0}, expectedTax={1}, expectedNet={2}")
    @DisplayName("processPayroll — PermanentEmployee tax calculation for various salaries")
    @CsvSource({
            "50000,   10000, 40000",
            "85000,   17000, 68000",
            "120000,  24000, 96000",
            "0,       0,     0"
    })
    void processPayroll_permanent_variousSalaries(double salary, double expectedTax, double expectedNet)
            throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "Test", "t@example.com",
                1, "Role", salary, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = payrollService.processPayroll(1, emp, PAYROLL_MONTH);
        assertEquals(salary,      record.grossSalary(), 0.01);
        assertEquals(expectedTax, record.taxAmount(),   0.01);
        assertEquals(expectedNet, record.netSalary(),   0.01);
    }

    // ── ContractEmployee — 10% tax ────────────────────────────────────────

    @Test
    @DisplayName("processPayroll — ContractEmployee applies 10% tax rate")
    void processPayroll_contract_tenPercentTax() throws Exception {
        ContractEmployee emp = new ContractEmployee(2, "Bob", "bob@example.com",
                10, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollRecord record = payrollService.processPayroll(2, emp, PAYROLL_MONTH);
        assertEquals(60_000.0, record.grossSalary());
        assertEquals(6_000.0,  record.taxAmount());
        assertEquals(54_000.0, record.netSalary());
    }

    @ParameterizedTest(name = "contract salary={0}, expectedTax={1}, expectedNet={2}")
    @DisplayName("processPayroll — ContractEmployee tax calculation for various salaries")
    @CsvSource({
            "40000,  4000,  36000",
            "60000,  6000,  54000",
            "100000, 10000, 90000"
    })
    void processPayroll_contract_variousSalaries(double salary, double expectedTax, double expectedNet)
            throws Exception {
        ContractEmployee emp = new ContractEmployee(2, "Test", "t@example.com",
                1, "Role", salary, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollRecord record = payrollService.processPayroll(1, emp, PAYROLL_MONTH);
        assertEquals(salary,      record.grossSalary(), 0.01);
        assertEquals(expectedTax, record.taxAmount(),   0.01);
        assertEquals(expectedNet, record.netSalary(),   0.01);
    }

    // ── PayrollRecord fields ──────────────────────────────────────────────

    @Test
    @DisplayName("processPayroll — PayrollRecord carries correct recordId and employeeId")
    void processPayroll_recordFields_correct() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(7, "Sara", "sara@example.com",
                1, "Analyst", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2021, 1, 1), false);
        PayrollRecord record = payrollService.processPayroll(42, emp, PAYROLL_MONTH);
        assertEquals(42,         record.id());
        assertEquals(7,          record.employeeId());
        assertEquals(PAYROLL_MONTH, record.payrollMonth());
        assertNotNull(record.processedTimestamp());
    }

    // ── PayrollException — expired contract ───────────────────────────────

    @Test
    @DisplayName("processPayroll — throws PayrollException for expired ContractEmployee")
    void processPayroll_expiredContract_throws() {
        ContractEmployee expired = new ContractEmployee(3, "Raj", "raj@example.com",
                20, "Designer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));
        PayrollException ex = assertThrows(PayrollException.class,
                () -> payrollService.processPayroll(1, expired, PAYROLL_MONTH));
        assertEquals(3, ex.getEmployeeId());
        assertTrue(ex.getMessage().contains("expired"));
    }

    // ── Null guards ───────────────────────────────────────────────────────

    @Test
    @DisplayName("processPayroll — throws NullPointerException for null employee")
    void processPayroll_nullEmployee_throws() {
        assertThrows(NullPointerException.class,
                () -> payrollService.processPayroll(1, null, PAYROLL_MONTH));
    }

    @Test
    @DisplayName("processPayroll — throws NullPointerException for null payrollMonth")
    void processPayroll_nullMonth_throws() {
        PermanentEmployee emp = new PermanentEmployee(1, "Alice", "alice@example.com",
                1, "Engineer", 80_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        assertThrows(NullPointerException.class,
                () -> payrollService.processPayroll(1, emp, null));
    }

    // ── processAll — batch ────────────────────────────────────────────────

    @Test
    @DisplayName("processAll — returns records for all eligible employees")
    void processAll_allEligible_returnsAllRecords() {
        PermanentEmployee e1 = new PermanentEmployee(1, "Alice", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        ContractEmployee e2 = new ContractEmployee(2, "Bob", "bob@example.com",
                10, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        List<PayrollRecord> records = payrollService.processAll(List.of(e1, e2), PAYROLL_MONTH);
        assertEquals(2, records.size());
    }

    @Test
    @DisplayName("processAll — skips expired ContractEmployee, continues for others")
    void processAll_expiredContractSkipped_othersProcessed() {
        PermanentEmployee valid = new PermanentEmployee(1, "Alice", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        ContractEmployee expired = new ContractEmployee(2, "Raj", "raj@example.com",
                20, "Designer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));
        List<PayrollRecord> records = payrollService.processAll(List.of(valid, expired), PAYROLL_MONTH);
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).employeeId());
    }

    @Test
    @DisplayName("processAll — returns empty list when all employees fail")
    void processAll_allFail_returnsEmpty() {
        ContractEmployee e1 = new ContractEmployee(1, "A", "a@example.com",
                1, "Role", 1000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        ContractEmployee e2 = new ContractEmployee(2, "B", "b@example.com",
                1, "Role", 1000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 2));
        List<PayrollRecord> records = payrollService.processAll(List.of(e1, e2), PAYROLL_MONTH);
        assertTrue(records.isEmpty());
    }

    @Test
    @DisplayName("processAll — returns empty list for empty input")
    void processAll_emptyInput_returnsEmpty() {
        assertTrue(payrollService.processAll(List.of(), PAYROLL_MONTH).isEmpty());
    }

    @Test
    @DisplayName("processAll — throws NullPointerException for null employees list")
    void processAll_nullList_throws() {
        assertThrows(NullPointerException.class,
                () -> payrollService.processAll(null, PAYROLL_MONTH));
    }

    @Test
    @DisplayName("processAll — throws NullPointerException for null payrollMonth")
    void processAll_nullMonth_throws() {
        assertThrows(NullPointerException.class,
                () -> payrollService.processAll(List.of(), null));
    }

    // ── netSalary consistency ─────────────────────────────────────────────

    @Test
    @DisplayName("processPayroll — netSalary always equals grossSalary minus taxAmount")
    void processPayroll_netSalaryConsistency() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "Consistency", "c@example.com",
                1, "Role", 73_456.78, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = payrollService.processPayroll(1, emp, PAYROLL_MONTH);
        assertEquals(record.grossSalary() - record.taxAmount(), record.netSalary(), 0.001);
    }
}

