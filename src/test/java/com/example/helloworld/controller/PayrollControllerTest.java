package com.example.helloworld.controller;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.PayrollException;
import com.example.helloworld.service.PayrollService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollController")
class PayrollControllerTest {

    @Mock
    private PayrollService payrollService;

    @InjectMocks
    private PayrollController controller;

    private static final LocalDate PAYROLL_MONTH = LocalDate.of(2026, 3, 1);

    private PermanentEmployee alice;
    private PayrollRecord      aliceRecord;

    @BeforeEach
    void setUp() {
        alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 80_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
        aliceRecord = new PayrollRecord(1, 1, 80_000, 16_000, 64_000,
                PAYROLL_MONTH, LocalDateTime.now());
    }

    // ── processPayroll ────────────────────────────────────────────────────────

    @Test
    @DisplayName("processPayroll — delegates to service and returns PayrollRecord")
    void processPayroll_delegates_returnsRecord() throws Exception {
        when(payrollService.processPayroll(1, alice, PAYROLL_MONTH)).thenReturn(aliceRecord);

        PayrollRecord result = controller.processPayroll(1, alice, PAYROLL_MONTH);

        assertNotNull(result);
        assertEquals(aliceRecord.grossSalary(), result.grossSalary(), 0.01);
        assertEquals(aliceRecord.taxAmount(),   result.taxAmount(),   0.01);
        assertEquals(aliceRecord.netSalary(),   result.netSalary(),   0.01);
        verify(payrollService).processPayroll(1, alice, PAYROLL_MONTH);
    }

    @Test
    @DisplayName("processPayroll — PayrollException is handled; returns null, no throw to caller")
    void processPayroll_payrollException_returnsNull() throws Exception {
        doThrow(new PayrollException(1, "contract expired"))
                .when(payrollService).processPayroll(1, alice, PAYROLL_MONTH);

        PayrollRecord result = assertDoesNotThrow(
                () -> controller.processPayroll(1, alice, PAYROLL_MONTH));

        assertNull(result);
        verify(payrollService).processPayroll(1, alice, PAYROLL_MONTH);
    }

    // ── processAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processAll — delegates to service and returns all records")
    void processAll_delegates_returnsRecords() {
        List<Employee> employees = List.of(alice);
        when(payrollService.processAll(employees, PAYROLL_MONTH)).thenReturn(List.of(aliceRecord));

        List<PayrollRecord> results = controller.processAll(employees, PAYROLL_MONTH);

        assertEquals(1, results.size());
        assertEquals(aliceRecord.employeeId(), results.get(0).employeeId());
        verify(payrollService).processAll(employees, PAYROLL_MONTH);
    }

    @Test
    @DisplayName("processAll — returns empty list when no records processed")
    void processAll_noRecords_returnsEmpty() {
        List<Employee> employees = List.of(alice);
        when(payrollService.processAll(employees, PAYROLL_MONTH)).thenReturn(List.of());

        List<PayrollRecord> results = controller.processAll(employees, PAYROLL_MONTH);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("processAll — throws NullPointerException when employees is null")
    void processAll_nullEmployees_throws() {
        assertThrows(NullPointerException.class,
                () -> controller.processAll(null, PAYROLL_MONTH));
    }

    @Test
    @DisplayName("processAll — throws NullPointerException when payrollMonth is null")
    void processAll_nullMonth_throws() {
        assertThrows(NullPointerException.class,
                () -> controller.processAll(List.of(alice), null));
    }
}

