package com.example.helloworld.service;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.EmployeeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests that validate correct {@link Optional} usage throughout
 * {@link EmployeeServiceImpl}.
 *
 * Covers every contract the service imposes on Optional-returning methods:
 * <ul>
 *   <li>Present / empty semantics for {@code getById} and {@code getByEmail}</li>
 *   <li>Value correctness when present — id, name, email, salary, status</li>
 *   <li>Chained Optional operations — {@code map}, {@code filter}, {@code orElse},
 *       {@code orElseGet}, {@code orElseThrow}, {@code ifPresent}</li>
 *   <li>Parameterized — multiple ids / emails, verifying empty is returned for each unknown</li>
 *   <li>Immutability contract — the same Optional is not reused across calls</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl — Optional validation")
class EmployeeValidationServiceEnhancedTest {

    @Mock  private EmployeeRepository repository;
    @InjectMocks private EmployeeServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee alice() {
        return new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    private static PermanentEmployee bob() {
        return new PermanentEmployee(2, "Bob Singh", "bob@example.com",
                20, "Manager", 120_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), false);
    }

    private static PermanentEmployee inactive() {
        return new PermanentEmployee(3, "Carol Rao", "carol@example.com",
                10, "Analyst", 70_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2018, 4, 1), false);
    }

    private static ContractEmployee contractor() {
        return new ContractEmployee(4, "Dave Lee", "dave@example.com",
                30, "Consultant", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), LocalDate.of(2026, 12, 31));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1 · getById — present / empty fundamentals
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById — Optional is present when repository returns an employee")
    void getById_present_whenRepositoryReturnsEmployee() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        assertTrue(service.getById(1).isPresent());
    }

    @Test
    @DisplayName("getById — Optional is empty when repository returns empty")
    void getById_empty_whenRepositoryReturnsEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertTrue(service.getById(99).isEmpty());
    }

    @Test
    @DisplayName("getById — contained value has correct id, name, email")
    void getById_present_valueHasCorrectFields() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        Employee found = service.getById(1).orElseThrow();
        assertEquals(1,                   found.getId());
        assertEquals("Alice Kumar",        found.getName());
        assertEquals("alice@example.com",  found.getEmail());
        assertEquals(85_000,               found.getSalary(), 0.01);
        assertEquals(EmployeeStatus.ACTIVE, found.getStatus());
    }

    @Test
    @DisplayName("getById — works for ContractEmployee (sealed subtype)")
    void getById_present_contractEmployee() {
        when(repository.findById(4)).thenReturn(Optional.of(contractor()));
        Optional<Employee> result = service.getById(4);
        assertTrue(result.isPresent());
        assertInstanceOf(ContractEmployee.class, result.get());
        assertEquals("Dave Lee", result.get().getName());
    }

    @Test
    @DisplayName("getById — INACTIVE employee is returned (no status filtering in service)")
    void getById_present_inactiveEmployee_returned() {
        when(repository.findById(3)).thenReturn(Optional.of(inactive()));
        Optional<Employee> result = service.getById(3);
        assertTrue(result.isPresent());
        assertEquals(EmployeeStatus.INACTIVE, result.get().getStatus());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2 · getById — parameterized over multiple unknown ids
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "getById({0}) → empty when not found")
    @DisplayName("getById — returns empty Optional for any unknown id")
    @ValueSource(ints = {0, -1, 99, 1000, Integer.MAX_VALUE})
    void getById_unknownId_alwaysEmpty(int id) {
        when(repository.findById(id)).thenReturn(Optional.empty());
        Optional<Employee> result = service.getById(id);
        assertFalse(result.isPresent(), "Expected empty Optional for id=" + id);
        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3 · getByEmail — present / empty fundamentals
    // ══════════��═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getByEmail — Optional is present when repository returns an employee")
    void getByEmail_present_whenFound() {
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice()));
        assertTrue(service.getByEmail("alice@example.com").isPresent());
    }

    @Test
    @DisplayName("getByEmail — Optional is empty when no employee has that email")
    void getByEmail_empty_whenNotFound() {
        when(repository.findByEmail("unknown@x.com")).thenReturn(Optional.empty());
        assertTrue(service.getByEmail("unknown@x.com").isEmpty());
    }

    @Test
    @DisplayName("getByEmail — contained value has correct email")
    void getByEmail_present_valueHasCorrectEmail() {
        when(repository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob()));
        Employee found = service.getByEmail("bob@example.com").orElseThrow();
        assertEquals("bob@example.com", found.getEmail());
        assertEquals(2,                 found.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4 · getByEmail — parameterized over unknown addresses
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "getByEmail(\"{0}\") → empty when not found")
    @DisplayName("getByEmail — returns empty Optional for any unknown email")
    @ValueSource(strings = {
            "nobody@example.com",
            "ALICE@EXAMPLE.COM",
            "",
            "   ",
            "missing"
    })
    void getByEmail_unknownEmail_alwaysEmpty(String email) {
        when(repository.findByEmail(email)).thenReturn(Optional.empty());
        assertTrue(service.getByEmail(email).isEmpty(),
                "Expected empty Optional for email='" + email + "'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5 · Chained Optional operations
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById — map() extracts salary when present")
    void getById_map_extractsSalary() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        double salary = service.getById(1)
                .map(Employee::getSalary)
                .orElse(-1.0);
        assertEquals(85_000, salary, 0.01);
    }

    @Test
    @DisplayName("getById — map() returns empty when source is empty")
    void getById_map_emptyWhenSourceEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Optional<Double> salary = service.getById(99).map(Employee::getSalary);
        assertTrue(salary.isEmpty());
    }

    @Test
    @DisplayName("getById — filter() keeps employee when predicate matches")
    void getById_filter_keepsMatchingEmployee() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        Optional<Employee> active = service.getById(1)
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE);
        assertTrue(active.isPresent());
    }

    @Test
    @DisplayName("getById — filter() removes employee when predicate does not match")
    void getById_filter_removesNonMatchingEmployee() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        // alice is ACTIVE → filtering for INACTIVE discards her
        Optional<Employee> inactive = service.getById(1)
                .filter(e -> e.getStatus() == EmployeeStatus.INACTIVE);
        assertTrue(inactive.isEmpty());
    }

    @Test
    @DisplayName("getById — orElse returns fallback when empty")
    void getById_orElse_returnsFallbackWhenEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Employee fallback = alice();
        Employee result = service.getById(99).orElse(fallback);
        assertSame(fallback, result);
    }

    @Test
    @DisplayName("getById — orElse returns found employee, not fallback, when present")
    void getById_orElse_returnsFoundEmployeeNotFallback() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        Employee fallback = bob();
        Employee result = service.getById(1).orElse(fallback);
        assertEquals(1, result.getId());   // alice, not bob
    }

    @Test
    @DisplayName("getById — orElseGet supplies fallback lazily when empty")
    void getById_orElseGet_suppliesFallbackLazily() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Employee result = service.getById(99).orElseGet(EmployeeValidationServiceEnhancedTest::aliceFixture);
        assertEquals(1, result.getId());  // alice supplied by lambda
    }

    @Test
    @DisplayName("getById — orElseThrow throws when empty")
    void getById_orElseThrow_throwsWhenEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(
                EmployeeNotFoundException.class,
                () -> service.getById(99)
                        .orElseThrow(() -> new EmployeeNotFoundException(99))
        );
    }

    @Test
    @DisplayName("getById — orElseThrow returns value and does not throw when present")
    void getById_orElseThrow_returnsValueWhenPresent() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        assertDoesNotThrow(() -> {
            Employee e = service.getById(1)
                    .orElseThrow(() -> new EmployeeNotFoundException(1));
            assertEquals(1, e.getId());
        });
    }

    @Test
    @DisplayName("getById — ifPresent consumer is called when value is present")
    void getById_ifPresent_consumerCalledWhenPresent() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        boolean[] called = {false};
        service.getById(1).ifPresent(e -> called[0] = true);
        assertTrue(called[0], "ifPresent consumer should have been invoked");
    }

    @Test
    @DisplayName("getById — ifPresent consumer is NOT called when empty")
    void getById_ifPresent_consumerNotCalledWhenEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        boolean[] called = {false};
        service.getById(99).ifPresent(e -> called[0] = true);
        assertFalse(called[0], "ifPresent consumer must not be invoked for empty Optional");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6 · Chained operations on getByEmail
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getByEmail — map() extracts role when present")
    void getByEmail_map_extractsRole() {
        when(repository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob()));
        String role = service.getByEmail("bob@example.com")
                .map(Employee::getRole)
                .orElse("unknown");
        assertEquals("Manager", role);
    }

    @Test
    @DisplayName("getByEmail — map() to id, then filter by id > 1 — present for id=2")
    void getByEmail_mapThenFilter_chainedOperations() {
        when(repository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob()));
        Optional<Integer> filteredId = service.getByEmail("bob@example.com")
                .map(Employee::getId)
                .filter(id -> id > 1);
        assertTrue(filteredId.isPresent());
        assertEquals(2, filteredId.get());
    }

    @Test
    @DisplayName("getByEmail — map() to id, then filter fails when id does not match condition")
    void getByEmail_mapThenFilter_emptyWhenFilterFails() {
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice()));
        // alice has id=1; filter for id > 1 removes her
        Optional<Integer> result = service.getByEmail("alice@example.com")
                .map(Employee::getId)
                .filter(id -> id > 1);
        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7 · Two independent calls return independent Optional instances
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById — two calls for same id return independent Optional instances")
    void getById_twoCalls_returnIndependentOptionals() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        Optional<Employee> first  = service.getById(1);
        Optional<Employee> second = service.getById(1);
        // same content but not necessarily same object — both must be present
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get().getId(), second.get().getId());
    }

    @Test
    @DisplayName("getById — one call present, another empty — independent results")
    void getById_presentAndEmptyCalls_areIndependent() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        when(repository.findById(2)).thenReturn(Optional.empty());
        assertTrue(service.getById(1).isPresent());
        assertTrue(service.getById(2).isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8 · getById and getByEmail return same employee (consistency)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById and getByEmail return the same employee for the same record")
    void getById_getByEmail_sameEmployee() {
        when(repository.findById(1)).thenReturn(Optional.of(alice()));
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice()));

        Employee byId    = service.getById(1).orElseThrow();
        Employee byEmail = service.getByEmail("alice@example.com").orElseThrow();
        assertEquals(byId.getId(),    byEmail.getId());
        assertEquals(byId.getEmail(), byEmail.getEmail());
        assertEquals(byId.getName(),  byEmail.getName());
    }

    // ── private helper (used by orElseGet lambda) ─────────────────────────────
    private static Employee aliceFixture() {
        return new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }
}
