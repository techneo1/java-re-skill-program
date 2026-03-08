package com.example.helloworld.controller;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.exception.*;
import com.example.helloworld.service.EmployeeService;
import com.example.helloworld.service.ValidationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController")
class EmployeeControllerTest {

    @Mock
    private EmployeeService   employeeService;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private EmployeeController controller;

    private PermanentEmployee alice;

    @BeforeEach
    void setUp() {
        alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    // ── addEmployee ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("addEmployee — validates then delegates to service")
    void addEmployee_validatesAndDelegates() throws Exception {
        controller.addEmployee(alice);
        verify(validationService).validate(alice);
        verify(employeeService).addEmployee(alice);
    }

    @Test
    @DisplayName("addEmployee — ValidationException is handled; service is never called")
    void addEmployee_validationException_doesNotCallService() throws Exception {
        doThrow(new ValidationException("status", EmployeeStatus.INACTIVE, "cannot add INACTIVE"))
                .when(validationService).validate(alice);

        assertDoesNotThrow(() -> controller.addEmployee(alice));
        verify(validationService).validate(alice);
        verify(employeeService, never()).addEmployee(any());
    }

    @Test
    @DisplayName("addEmployee — DuplicateEmployeeException is handled; no throw to caller")
    void addEmployee_duplicateEmployee_handledSilently() throws Exception {
        doThrow(new DuplicateEmployeeException(1)).when(employeeService).addEmployee(alice);

        assertDoesNotThrow(() -> controller.addEmployee(alice));
        verify(validationService).validate(alice);
        verify(employeeService).addEmployee(alice);
    }

    @Test
    @DisplayName("addEmployee — DuplicateEmailException is handled; no throw to caller")
    void addEmployee_duplicateEmail_handledSilently() throws Exception {
        doThrow(new DuplicateEmailException("alice@example.com")).when(employeeService).addEmployee(alice);

        assertDoesNotThrow(() -> controller.addEmployee(alice));
    }

    // ── updateEmployee ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEmployee — validates then delegates to service")
    void updateEmployee_validatesAndDelegates() throws Exception {
        controller.updateEmployee(alice);
        verify(validationService).validate(alice);
        verify(employeeService).updateEmployee(alice);
    }

    @Test
    @DisplayName("updateEmployee — EmployeeNotFoundException is handled; no throw to caller")
    void updateEmployee_notFound_handledSilently() throws Exception {
        doThrow(new EmployeeNotFoundException(1)).when(employeeService).updateEmployee(alice);

        assertDoesNotThrow(() -> controller.updateEmployee(alice));
    }

    @Test
    @DisplayName("updateEmployee — ValidationException stops service call")
    void updateEmployee_validationException_doesNotCallService() throws Exception {
        doThrow(new ValidationException("email", "bad", "must contain '@'"))
                .when(validationService).validate(alice);

        assertDoesNotThrow(() -> controller.updateEmployee(alice));
        verify(employeeService, never()).updateEmployee(any());
    }

    // ── removeEmployee ────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeEmployee — delegates to service")
    void removeEmployee_delegates() throws Exception {
        controller.removeEmployee(1);
        verify(employeeService).removeEmployee(1);
    }

    @Test
    @DisplayName("removeEmployee — EmployeeNotFoundException is handled; no throw to caller")
    void removeEmployee_notFound_handledSilently() throws Exception {
        doThrow(new EmployeeNotFoundException(999)).when(employeeService).removeEmployee(999);

        assertDoesNotThrow(() -> controller.removeEmployee(999));
    }

    // ── queries ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — delegates to service and returns result")
    void getById_delegates() {
        when(employeeService.getById(1)).thenReturn(Optional.of(alice));
        Optional<Employee> result = controller.getById(1);
        assertTrue(result.isPresent());
        assertEquals(alice, result.get());
        verify(employeeService).getById(1);
    }

    @Test
    @DisplayName("getAllEmployees — delegates to service and returns result")
    void getAllEmployees_delegates() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(alice));
        List<Employee> result = controller.getAllEmployees();
        assertEquals(1, result.size());
        verify(employeeService).getAllEmployees();
    }

    @Test
    @DisplayName("getByDepartment — delegates to service")
    void getByDepartment_delegates() {
        when(employeeService.getByDepartment(10)).thenReturn(List.of(alice));
        List<Employee> result = controller.getByDepartment(10);
        assertEquals(1, result.size());
        verify(employeeService).getByDepartment(10);
    }

    @Test
    @DisplayName("getBySalaryRange — delegates to service when range is valid")
    void getBySalaryRange_valid_delegates() throws Exception {
        when(employeeService.getBySalaryRange(50_000, 100_000)).thenReturn(List.of(alice));
        List<Employee> result = controller.getBySalaryRange(50_000, 100_000);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getBySalaryRange — InvalidEmployeeDataException returns empty list, no throw")
    void getBySalaryRange_invalid_returnsEmptyList() throws Exception {
        doThrow(new InvalidEmployeeDataException("min", -1000, "must not be negative"))
                .when(employeeService).getBySalaryRange(-1000, 90_000);

        List<Employee> result = assertDoesNotThrow(() -> controller.getBySalaryRange(-1000, 90_000));
        assertTrue(result.isEmpty());
    }

    // ── aggregations ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("countEmployees — delegates to service")
    void countEmployees_delegates() {
        when(employeeService.countEmployees()).thenReturn(3);
        assertEquals(3, controller.countEmployees());
        verify(employeeService).countEmployees();
    }

    @Test
    @DisplayName("totalSalary — delegates to service")
    void totalSalary_delegates() {
        when(employeeService.totalSalary()).thenReturn(285_000.0);
        assertEquals(285_000.0, controller.totalSalary(), 0.01);
        verify(employeeService).totalSalary();
    }

    @Test
    @DisplayName("averageSalary — delegates to service")
    void averageSalary_delegates() {
        when(employeeService.averageSalary()).thenReturn(95_000.0);
        assertEquals(95_000.0, controller.averageSalary(), 0.01);
        verify(employeeService).averageSalary();
    }
}

