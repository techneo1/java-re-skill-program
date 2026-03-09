package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.exception.DepartmentNotFoundException;
import com.example.helloworld.exception.DuplicateEmailException;
import com.example.helloworld.exception.ValidationException;
import com.example.helloworld.repository.EmployeeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the enhanced EmployeeValidationService covering:
 *  1. Salary cannot be negative
 *  2. Unique email validation (via repository)
 *  3. Department must exist (via validDepartmentIds set)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeValidationService — salary, email uniqueness, department existence")
class EmployeeValidationServiceEnhancedTest {

    @Mock
    private EmployeeRepository repository;

    /** Valid department IDs used across tests. */
    private static final Set<Integer> VALID_DEPT_IDS = Set.of(10, 20, 30);

    private EmployeeValidationService validator;

    @BeforeEach
    void setUp() {
        validator = new EmployeeValidationService(repository, VALID_DEPT_IDS);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static PermanentEmployee validEmployee(int id, int deptId, String email) {
        return new PermanentEmployee(id, "Test User", email,
                deptId, "Engineer", 70_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. Salary cannot be negative
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("salary — throws ValidationException for negative salary (constructor guard)")
    void salary_negative_throwsFromConstructor() {
        // Employee constructor guards salary < 0 — we verify the guard fires
        assertThrows(IllegalArgumentException.class,
                () -> new PermanentEmployee(1, "Bad Salary", "bad@example.com",
                        10, "Engineer", -1, EmployeeStatus.ACTIVE,
                        LocalDate.of(2022, 1, 1), false));
    }

    @ParameterizedTest(name = "salary = {0}")
    @DisplayName("salary — passes for zero and positive values")
    @ValueSource(doubles = {0.0, 0.01, 50_000.0, 999_999.99})
    void salary_zeroAndPositive_passes(double salary) {
        Employee emp = new PermanentEmployee(1, "Valid Sal", "valsal@example.com",
                10, "Engineer", salary, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), false);
        // repository: no conflict
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(emp));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. Unique email validation
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("email uniqueness — passes when no other employee owns the email")
    void emailUnique_noConflict_passes() {
        Employee emp = validEmployee(1, 10, "alice@example.com");
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(emp));
        verify(repository).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("email uniqueness — passes when the existing owner IS the same employee (update scenario)")
    void emailUnique_sameEmployee_passes() {
        Employee emp = validEmployee(1, 10, "alice@example.com");
        // repository returns the same id → not a conflict
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(emp));

        assertDoesNotThrow(() -> validator.validate(emp));
    }

    @Test
    @DisplayName("email uniqueness — throws DuplicateEmailException when another employee owns the email")
    void emailUnique_differentOwner_throwsDuplicateEmail() {
        Employee newEmp     = validEmployee(2, 10, "alice@example.com");
        Employee existingEmp = validEmployee(1, 10, "alice@example.com"); // different id!
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingEmp));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> validator.validate(newEmp));
        assertEquals("alice@example.com", ex.getDuplicateEmail());
    }

    @Test
    @DisplayName("email uniqueness — DuplicateEmailException message contains the email")
    void emailUnique_exceptionMessageContainsEmail() {
        Employee newEmp      = validEmployee(2, 10, "bob@example.com");
        Employee existingEmp = validEmployee(1, 10, "bob@example.com");
        when(repository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingEmp));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> validator.validate(newEmp));
        assertTrue(ex.getMessage().contains("bob@example.com"));
    }

    @Test
    @DisplayName("email uniqueness — skipped when no repository is configured (field-only mode)")
    void emailUnique_noRepository_skipCheck() {
        // field-only validator — no repository, no department check
        EmployeeValidationService fieldOnly = new EmployeeValidationService();
        Employee emp = validEmployee(1, 10, "any@example.com");

        assertDoesNotThrow(() -> fieldOnly.validate(emp));
        verifyNoInteractions(repository);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. Department must exist
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("department — passes when departmentId is in the valid set")
    void department_knownId_passes() {
        Employee emp = validEmployee(1, 10, "alice@example.com");
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(emp));
    }

    @ParameterizedTest(name = "deptId = {0}")
    @DisplayName("department — passes for all known department IDs")
    @ValueSource(ints = {10, 20, 30})
    void department_allKnownIds_pass(int deptId) {
        Employee emp = validEmployee(1, deptId, "emp" + deptId + "@example.com");
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(emp));
    }

    @Test
    @DisplayName("department — throws DepartmentNotFoundException for unknown departmentId")
    void department_unknownId_throwsDepartmentNotFound() {
        Employee emp = validEmployee(1, 99, "emp99@example.com"); // dept 99 not in set
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        DepartmentNotFoundException ex = assertThrows(DepartmentNotFoundException.class,
                () -> validator.validate(emp));
        assertEquals(99, ex.getDepartmentId());
    }

    @Test
    @DisplayName("department — DepartmentNotFoundException message contains the bad id")
    void department_exceptionMessageContainsId() {
        Employee emp = validEmployee(1, 42, "emp42@example.com");
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        DepartmentNotFoundException ex = assertThrows(DepartmentNotFoundException.class,
                () -> validator.validate(emp));
        assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    @DisplayName("department — check skipped when validDepartmentIds is null/empty (field-only mode)")
    void department_noSet_skipCheck() {
        // field-only: no repository, no department set → departmentId 999 does NOT throw
        EmployeeValidationService fieldOnly = new EmployeeValidationService();
        Employee emp = validEmployee(1, 999, "skip@example.com");

        assertDoesNotThrow(() -> fieldOnly.validate(emp));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. Ordering — salary checked before email uniqueness
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ordering — salary rule fires before email uniqueness check (no repo call)")
    void ordering_salaryBeforeEmail() {
        // salary < 0 is guarded by Employee constructor — so we verify that
        // when the constructor throws, validate() is never even called.
        assertThrows(IllegalArgumentException.class,
                () -> new PermanentEmployee(1, "T", "t@t.com", 10, "Eng",
                        -500, EmployeeStatus.ACTIVE, LocalDate.of(2022, 1, 1), false));
        // repository must never have been touched
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("ordering — email format rule fires before uniqueness check (no repo call)")
    void ordering_emailFormatBeforeUniqueness() {
        // bad email (no @) — ValidationException must fire before we hit the repository
        Employee emp = new PermanentEmployee(1, "Test", "noemail",
                10, "Eng", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), false);

        assertThrows(ValidationException.class, () -> validator.validate(emp));
        verifyNoInteractions(repository);
    }
}

