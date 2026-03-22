package com.example.helloworld.db;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.*;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link JdbcEmployeeDao}.
 *
 * Uses an H2 in-memory database so no external DB is required.
 * Each test runs against a fresh schema (tables truncated in @BeforeEach).
 *
 * Covers:
 * - PreparedStatement INSERT / UPDATE / DELETE
 * - ResultSet mapping (PermanentEmployee and ContractEmployee)
 * - Duplicate ID / e-mail guards
 * - All query methods (findByDepartment, findByStatus, findByRole,
 *   findByEmail, findBySalaryRange, count, totalSalary, averageSalary)
 */
@DisplayName("JdbcEmployeeDao")
class JdbcEmployeeDaoTest {

    private static final String URL  = "jdbc:h2:mem:emp_dao_test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private DataSourceFactory dsf;
    private JdbcEmployeeDao   dao;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee alice() {
        return new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    private static PermanentEmployee bob() {
        return new PermanentEmployee(2, "Bob Smith", "bob@example.com",
                10, "Manager", 120_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), false);
    }

    private static ContractEmployee carol() {
        return new ContractEmployee(3, "Carol White", "carol@example.com",
                20, "Consultant", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));
    }

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws SQLException {
        dsf = new DataSourceFactory(URL, USER, PASS);
        dsf.initSchema();
        dao = new JdbcEmployeeDao(dsf);
        // Truncate in FK-safe order for test isolation
        try (var con = dsf.getConnection(); var stmt = con.createStatement()) {
            stmt.execute("DELETE FROM payroll_records");
            stmt.execute("DELETE FROM employees");
            stmt.execute("DELETE FROM departments");
            // seed departments referenced by fixtures (dept 10, dept 20)
            stmt.execute("INSERT INTO departments (id, name, location) VALUES (10, 'Engineering', 'Bangalore')");
            stmt.execute("INSERT INTO departments (id, name, location) VALUES (20, 'Consulting', 'Mumbai')");
        }
    }

    // ── add() ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add() persists a PermanentEmployee — findById returns it")
    void add_permanent_persistsAndRetrievable() throws Exception {
        dao.add(alice());
        Optional<Employee> found = dao.findById(1);
        assertTrue(found.isPresent());
        assertInstanceOf(PermanentEmployee.class, found.get());
        assertEquals("Alice Kumar", found.get().getName());
        assertEquals(85_000, found.get().getSalary());
        assertTrue(((PermanentEmployee) found.get()).isGratuityEligible());
    }

    @Test
    @DisplayName("add() persists a ContractEmployee — contractEndDate is preserved")
    void add_contract_persistsAndRetrievable() throws Exception {
        dao.add(carol());
        Optional<Employee> found = dao.findById(3);
        assertTrue(found.isPresent());
        assertInstanceOf(ContractEmployee.class, found.get());
        ContractEmployee ce = (ContractEmployee) found.get();
        assertEquals(LocalDate.of(2025, 12, 31), ce.getContractEndDate());
    }

    @Test
    @DisplayName("add() throws DuplicateEmployeeException for duplicate ID")
    void add_duplicateId_throws() throws Exception {
        dao.add(alice());
        assertThrows(DuplicateEmployeeException.class, () -> dao.add(alice()));
    }

    @Test
    @DisplayName("add() throws DuplicateEmailException for duplicate e-mail")
    void add_duplicateEmail_throws() throws Exception {
        dao.add(alice());
        // same e-mail, different ID
        PermanentEmployee twin = new PermanentEmployee(99, "Alice Twin", "alice@example.com",
                10, "Engineer", 70_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2021, 1, 1), false);
        assertThrows(DuplicateEmailException.class, () -> dao.add(twin));
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() persists salary change")
    void update_salaryChange_persisted() throws Exception {
        dao.add(alice());
        Employee stored = dao.findById(1).orElseThrow();
        stored.setSalary(95_000);
        dao.update(stored);
        assertEquals(95_000, dao.findById(1).orElseThrow().getSalary());
    }

    @Test
    @DisplayName("update() throws EmployeeNotFoundException for unknown id")
    void update_unknownId_throws() {
        assertThrows(EmployeeNotFoundException.class, () -> dao.update(alice()));
    }

    @Test
    @DisplayName("update() throws DuplicateEmailException when e-mail clashes with another employee")
    void update_emailClash_throws() throws Exception {
        dao.add(alice());
        dao.add(bob());
        Employee stored = dao.findById(2).orElseThrow();
        // try to change bob's email to alice's
        PermanentEmployee updated = new PermanentEmployee(stored.getId(), stored.getName(),
                "alice@example.com", stored.getDepartmentId(), stored.getRole(),
                stored.getSalary(), stored.getStatus(), stored.getJoiningDate(),
                ((PermanentEmployee) stored).isGratuityEligible());
        assertThrows(DuplicateEmailException.class, () -> dao.update(updated));
    }

    // ── remove() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove() deletes an employee — findById returns empty")
    void remove_deletesEmployee() throws Exception {
        dao.add(alice());
        dao.remove(1);
        assertTrue(dao.findById(1).isEmpty());
    }

    @Test
    @DisplayName("remove() throws EmployeeNotFoundException for unknown id")
    void remove_unknownId_throws() {
        assertThrows(EmployeeNotFoundException.class, () -> dao.remove(999));
    }

    // ── findAll() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() returns all employees")
    void findAll_returnsAll() throws Exception {
        dao.add(alice());
        dao.add(bob());
        dao.add(carol());
        assertEquals(3, dao.findAll().size());
    }

    @Test
    @DisplayName("findAll() returns empty list when no employees")
    void findAll_empty() {
        assertTrue(dao.findAll().isEmpty());
    }

    // ── findByDepartment() ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByDepartment() returns only employees in that department")
    void findByDepartment_filtersCorrectly() throws Exception {
        dao.add(alice());   // dept 10
        dao.add(bob());     // dept 10
        dao.add(carol());   // dept 20
        List<Employee> dept10 = dao.findByDepartment(10);
        assertEquals(2, dept10.size());
        assertTrue(dept10.stream().allMatch(e -> e.getDepartmentId() == 10));
    }

    // ── findByStatus() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus() filters by ACTIVE / INACTIVE")
    void findByStatus_filtersCorrectly() throws Exception {
        dao.add(alice());
        PermanentEmployee inactive = new PermanentEmployee(4, "Dave", "dave@example.com",
                10, "Analyst", 50_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2018, 1, 1), false);
        dao.add(inactive);

        List<Employee> active = dao.findByStatus(EmployeeStatus.ACTIVE);
        assertEquals(1, active.size());
        assertEquals(1, active.get(0).getId());
    }

    // ── findByRole() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByRole() returns only matching roles")
    void findByRole_filtersCorrectly() throws Exception {
        dao.add(alice());   // Engineer
        dao.add(bob());     // Manager
        List<Employee> engineers = dao.findByRole("Engineer");
        assertEquals(1, engineers.size());
        assertEquals("Alice Kumar", engineers.get(0).getName());
    }

    // ── findByEmail() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail() finds employee case-insensitively")
    void findByEmail_caseInsensitive() throws Exception {
        dao.add(alice());
        assertTrue(dao.findByEmail("ALICE@EXAMPLE.COM").isPresent());
    }

    @Test
    @DisplayName("findByEmail() returns empty for unknown email")
    void findByEmail_notFound() {
        assertTrue(dao.findByEmail("nobody@example.com").isEmpty());
    }

    // ── findBySalaryRange() ───────────────────────────────────────────────────

    @Test
    @DisplayName("findBySalaryRange() returns employees within bounds (inclusive)")
    void findBySalaryRange_inclusive() throws Exception {
        dao.add(alice());    // 85 000
        dao.add(bob());      // 120 000
        dao.add(carol());    // 60 000
        List<Employee> result = dao.findBySalaryRange(60_000, 90_000);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findBySalaryRange() throws InvalidEmployeeDataException for invalid range")
    void findBySalaryRange_invalidRange_throws() {
        assertThrows(InvalidEmployeeDataException.class,
                () -> dao.findBySalaryRange(100_000, 50_000));
    }

    // ── Aggregate queries ─────────────────────────────────────────────────────

    @Test
    @DisplayName("count() returns correct number of rows")
    void count_returnsCorrectCount() throws Exception {
        assertEquals(0, dao.count());
        dao.add(alice());
        dao.add(bob());
        assertEquals(2, dao.count());
    }

    @Test
    @DisplayName("totalSalary() returns sum of all salaries")
    void totalSalary_returnsSum() throws Exception {
        dao.add(alice());   // 85 000
        dao.add(bob());     // 120 000
        assertEquals(205_000, dao.totalSalary(), 0.01);
    }

    @Test
    @DisplayName("averageSalary() returns correct average")
    void averageSalary_returnsAverage() throws Exception {
        dao.add(alice());   // 85 000
        dao.add(bob());     // 120 000
        assertEquals(102_500, dao.averageSalary(), 0.01);
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add() throws NullPointerException for null employee")
    void add_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.add(null));
    }
}
