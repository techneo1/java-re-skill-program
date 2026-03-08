package com.example.helloworld.domain.payroll;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.PayrollException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PayrollStrategy implementations")
class PayrollStrategyTest {

    private static final LocalDate PAYROLL_MONTH = LocalDate.of(2026, 3, 1);

    private PermanentEmployeePayrollStrategy permanentStrategy;
    private ContractEmployeePayrollStrategy  contractStrategy;

    // ── Lifecycle hooks ───────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        permanentStrategy = new PermanentEmployeePayrollStrategy();
        contractStrategy  = new ContractEmployeePayrollStrategy();
    }

    // ── PermanentEmployeePayrollStrategy ──────────────────────────────────

    @Test
    @DisplayName("permanent — calculates 20% tax correctly")
    void permanent_calculate_twentyPercentTax() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "Alice", "alice@example.com",
                1, "Engineer", 100_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = permanentStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(100_000.0, record.grossSalary());
        assertEquals(20_000.0,  record.taxAmount());
        assertEquals(80_000.0,  record.netSalary());
    }

    @ParameterizedTest(name = "gross={0} → tax={1}, net={2}")
    @DisplayName("permanent — 20% tax across various salaries")
    @CsvSource({
            "0,       0,      0",
            "10000,   2000,   8000",
            "50000,   10000,  40000",
            "100000,  20000,  80000",
            "150000,  30000,  120000"
    })
    void permanent_calculate_variousSalaries(double gross, double tax, double net) throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "T", "t@e.com",
                1, "R", gross, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = permanentStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(gross, record.grossSalary(), 0.01);
        assertEquals(tax,   record.taxAmount(),   0.01);
        assertEquals(net,   record.netSalary(),   0.01);
    }

    @Test
    @DisplayName("permanent — throws PayrollException when passed a ContractEmployee")
    void permanent_wrongType_throws() {
        ContractEmployee ce = new ContractEmployee(2, "Bob", "bob@example.com",
                1, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollException ex = assertThrows(PayrollException.class,
                () -> permanentStrategy.calculate(1, ce, PAYROLL_MONTH));
        assertEquals(2, ex.getEmployeeId());
        assertTrue(ex.getMessage().contains("PermanentEmployeePayrollStrategy cannot process"));
    }

    @Test
    @DisplayName("permanent — PayrollRecord carries correct recordId and employeeId")
    void permanent_recordFields() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(5, "Sara", "sara@example.com",
                1, "Analyst", 80_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2021, 1, 1), false);
        PayrollRecord record = permanentStrategy.calculate(99, emp, PAYROLL_MONTH);
        assertEquals(99,           record.id());
        assertEquals(5,            record.employeeId());
        assertEquals(PAYROLL_MONTH, record.payrollMonth());
        assertNotNull(record.processedTimestamp());
    }

    // ── ContractEmployeePayrollStrategy ───────────────────────────────────

    @Test
    @DisplayName("contract — calculates 10% tax correctly")
    void contract_calculate_tenPercentTax() throws Exception {
        ContractEmployee emp = new ContractEmployee(2, "Bob", "bob@example.com",
                1, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollRecord record = contractStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(60_000.0, record.grossSalary());
        assertEquals(6_000.0,  record.taxAmount());
        assertEquals(54_000.0, record.netSalary());
    }

    @ParameterizedTest(name = "gross={0} → tax={1}, net={2}")
    @DisplayName("contract — 10% tax across various salaries")
    @CsvSource({
            "0,       0,     0",
            "10000,   1000,  9000",
            "50000,   5000,  45000",
            "100000,  10000, 90000"
    })
    void contract_calculate_variousSalaries(double gross, double tax, double net) throws Exception {
        ContractEmployee emp = new ContractEmployee(2, "T", "t@e.com",
                1, "R", gross, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollRecord record = contractStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(gross, record.grossSalary(), 0.01);
        assertEquals(tax,   record.taxAmount(),   0.01);
        assertEquals(net,   record.netSalary(),   0.01);
    }

    @Test
    @DisplayName("contract — throws PayrollException for expired contract")
    void contract_expired_throws() {
        ContractEmployee expired = new ContractEmployee(3, "Raj", "raj@example.com",
                1, "Designer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));
        PayrollException ex = assertThrows(PayrollException.class,
                () -> contractStrategy.calculate(1, expired, PAYROLL_MONTH));
        assertEquals(3, ex.getEmployeeId());
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("contract — throws PayrollException when passed a PermanentEmployee")
    void contract_wrongType_throws() {
        PermanentEmployee pe = new PermanentEmployee(1, "Alice", "alice@example.com",
                1, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 1, 1), true);
        PayrollException ex = assertThrows(PayrollException.class,
                () -> contractStrategy.calculate(1, pe, PAYROLL_MONTH));
        assertEquals(1, ex.getEmployeeId());
        assertTrue(ex.getMessage().contains("ContractEmployeePayrollStrategy cannot process"));
    }

    @Test
    @DisplayName("contract — passes for contract expiring tomorrow (boundary)")
    void contract_expiringTomorrow_passes() throws Exception {
        ContractEmployee emp = new ContractEmployee(4, "Lisa", "lisa@example.com",
                1, "Dev", 70_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> contractStrategy.calculate(1, emp, PAYROLL_MONTH));
    }

    // ── netSalary consistency ─────────────────────────────────────────────

    @Test
    @DisplayName("permanent — netSalary always equals grossSalary minus taxAmount")
    void permanent_netSalaryConsistency() throws Exception {
        PermanentEmployee emp = new PermanentEmployee(1, "C", "c@example.com",
                1, "R", 73_456.78, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), true);
        PayrollRecord record = permanentStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(record.grossSalary() - record.taxAmount(), record.netSalary(), 0.001);
    }

    @Test
    @DisplayName("contract — netSalary always equals grossSalary minus taxAmount")
    void contract_netSalaryConsistency() throws Exception {
        ContractEmployee emp = new ContractEmployee(2, "C", "c@example.com",
                1, "R", 73_456.78, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        PayrollRecord record = contractStrategy.calculate(1, emp, PAYROLL_MONTH);
        assertEquals(record.grossSalary() - record.taxAmount(), record.netSalary(), 0.001);
    }
}

