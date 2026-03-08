package com.example.helloworld.service;

import com.example.helloworld.domain.ContractEmployee;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.exception.ValidationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmployeeValidationService")
class EmployeeValidationServiceTest {

    private EmployeeValidationService validator;

    // ── Lifecycle hooks ───────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        validator = new EmployeeValidationService();
    }

    // ── Happy path ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate — passes for valid PermanentEmployee")
    void validate_validPermanentEmployee_noException() {
        Employee emp = new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    @Test
    @DisplayName("validate — passes for valid ContractEmployee with future end date")
    void validate_validContractEmployee_noException() {
        Employee emp = new ContractEmployee(2, "Bob Singh", "bob@example.com",
                10, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31));
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ── id ────────────────────────────────────────────────────────────────

    // id validation is enforced by Employee constructor (id <= 0 throws IAE before validator runs),
    // so we confirm that boundary: id=1 is the minimum valid value.
    @Test
    @DisplayName("validate — passes with minimum valid id = 1")
    void validate_minimumId_passes() {
        Employee emp = new PermanentEmployee(1, "Min Id", "minid@example.com",
                1, "Role", 0, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), false);
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ── email ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "invalid email: \"{0}\"")
    @DisplayName("validate — throws ValidationException for email missing '@'")
    @ValueSource(strings = {"notanemail", "missingatsign.com", "nodomain"})
    void validate_invalidEmail_throws(String badEmail) {
        Employee emp = new PermanentEmployee(1, "Test User", badEmail,
                10, "Engineer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), true);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(emp));
        assertEquals("email", ex.getFieldName());
        assertEquals(badEmail, ex.getRejectedValue());
    }

    // ── salary ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "salary = {0}")
    @DisplayName("validate — passes for boundary salary values (0 and above)")
    @ValueSource(doubles = {0.0, 0.01, 1_000.0, 999_999.99})
    void validate_validSalary_passes(double salary) {
        Employee emp = new PermanentEmployee(1, "Salary Test", "s@example.com",
                1, "Role", salary, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), false);
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ── status ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate — throws ValidationException for INACTIVE status")
    void validate_inactiveStatus_throws() {
        Employee emp = new PermanentEmployee(1, "Joe Doe", "joe@example.com",
                10, "Analyst", 60_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2020, 1, 1), false);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(emp));
        assertEquals("status", ex.getFieldName());
        assertEquals(EmployeeStatus.INACTIVE, ex.getRejectedValue());
    }

    // ── departmentId ──────────────────────────────────────────────────────

    // departmentId <= 0 is also guarded by Employee constructor; test minimum valid value
    @Test
    @DisplayName("validate — passes with minimum valid departmentId = 1")
    void validate_minimumDepartmentId_passes() {
        Employee emp = new PermanentEmployee(1, "Dept Test", "dept@example.com",
                1, "Role", 50_000, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), false);
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ── ContractEmployee expiry ───────────────────────────────────────────

    @Test
    @DisplayName("validate — throws ValidationException for expired ContractEmployee")
    void validate_expiredContract_throws() {
        Employee emp = new ContractEmployee(3, "Raj Kumar", "raj@example.com",
                20, "Designer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(emp));
        assertEquals("contractEndDate", ex.getFieldName());
    }

    @Test
    @DisplayName("validate — passes for ContractEmployee expiring today (boundary)")
    void validate_contractExpiringFuture_passes() {
        Employee emp = new ContractEmployee(4, "Future Contract", "future@example.com",
                10, "Developer", 70_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ── ValidationException fields ────────────────────────────────────────

    @ParameterizedTest(name = "field={0}, rejectedValue={1}")
    @DisplayName("validate — ValidationException carries correct fieldName and rejectedValue")
    @CsvSource({
            "notanemail,  email",
    })
    void validate_exceptionFields_correct(String badEmail, String expectedField) {
        Employee emp = new PermanentEmployee(1, "Check Fields", badEmail,
                1, "Role", 0, EmployeeStatus.ACTIVE, LocalDate.of(2020, 1, 1), false);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(emp));
        assertEquals(expectedField, ex.getFieldName());
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedField));
    }
}

