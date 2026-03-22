package com.example.helloworld.db;

import com.example.helloworld.domain.Department;
import com.example.helloworld.exception.DepartmentNotFoundException;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link JdbcDepartmentDao}.
 *
 * Uses an H2 in-memory database — no external DB required.
 * Each test runs against a clean departments table (wiped in @BeforeEach).
 *
 * Covers:
 * - PreparedStatement INSERT / UPDATE / DELETE
 * - ResultSet mapping to {@link Department} record
 * - Duplicate id guard
 * - All query methods: findById, findByName, findAll, findByLocation, count
 * - DepartmentNotFoundException for update / remove on unknown ids
 */
@DisplayName("JdbcDepartmentDao")
class JdbcDepartmentDaoTest {

    private static final String URL  = "jdbc:h2:mem:dept_dao_test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private DataSourceFactory  dsf;
    private JdbcDepartmentDao  dao;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static Department engineering() {
        return new Department(10, "Engineering", "Bangalore");
    }

    private static Department hr() {
        return new Department(20, "Human Resources", "Mumbai");
    }

    private static Department finance() {
        return new Department(30, "Finance", "Mumbai");
    }

    private static Department sales() {
        return new Department(40, "Sales", "Delhi");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws SQLException {
        dsf = new DataSourceFactory(URL, USER, PASS);
        dsf.initSchema();
        dao = new JdbcDepartmentDao(dsf);
        // Wipe in FK-safe order for test isolation
        try (var con = dsf.getConnection(); var stmt = con.createStatement()) {
            stmt.execute("DELETE FROM payroll_records");
            stmt.execute("DELETE FROM employees");
            stmt.execute("DELETE FROM departments");
        }
    }

    // ── add() ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add() persists a department — findById returns it")
    void add_persistsDepartment() {
        dao.add(engineering());
        Optional<Department> found = dao.findById(10);
        assertTrue(found.isPresent());
        assertEquals("Engineering", found.get().name());
        assertEquals("Bangalore",   found.get().location());
    }

    @Test
    @DisplayName("add() throws IllegalArgumentException for duplicate id")
    void add_duplicateId_throws() {
        dao.add(engineering());
        assertThrows(IllegalArgumentException.class, () -> dao.add(engineering()));
    }

    @Test
    @DisplayName("add() throws NullPointerException for null department")
    void add_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.add(null));
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() persists name and location changes")
    void update_persistsChanges() throws Exception {
        dao.add(engineering());
        Department updated = new Department(10, "R&D Engineering", "Hyderabad");
        dao.update(updated);

        Department found = dao.findById(10).orElseThrow();
        assertEquals("R&D Engineering", found.name());
        assertEquals("Hyderabad",       found.location());
    }

    @Test
    @DisplayName("update() throws DepartmentNotFoundException for unknown id")
    void update_unknownId_throws() {
        assertThrows(DepartmentNotFoundException.class,
                () -> dao.update(engineering()));
    }

    @Test
    @DisplayName("update() throws NullPointerException for null department")
    void update_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.update(null));
    }

    // ── remove() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove() deletes a department — findById returns empty")
    void remove_deletesDepartment() throws Exception {
        dao.add(engineering());
        dao.remove(10);
        assertTrue(dao.findById(10).isEmpty());
    }

    @Test
    @DisplayName("remove() throws DepartmentNotFoundException for unknown id")
    void remove_unknownId_throws() {
        assertThrows(DepartmentNotFoundException.class, () -> dao.remove(999));
    }

    // ── findById() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() returns empty for unknown id")
    void findById_notFound_returnsEmpty() {
        assertTrue(dao.findById(999).isEmpty());
    }

    // ── findByName() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByName() finds department case-insensitively")
    void findByName_caseInsensitive() {
        dao.add(engineering());
        assertTrue(dao.findByName("engineering").isPresent());
        assertTrue(dao.findByName("ENGINEERING").isPresent());
    }

    @Test
    @DisplayName("findByName() returns empty for unknown name")
    void findByName_notFound_returnsEmpty() {
        assertTrue(dao.findByName("Unknown Dept").isEmpty());
    }

    @Test
    @DisplayName("findByName() throws NullPointerException for null name")
    void findByName_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.findByName(null));
    }

    // ── findAll() ───────────────────────────────────���────────────────────────

    @Test
    @DisplayName("findAll() returns all departments ordered by id")
    void findAll_returnsAllOrderedById() {
        dao.add(finance());      // id 30
        dao.add(engineering());  // id 10
        dao.add(hr());           // id 20
        List<Department> all = dao.findAll();
        assertEquals(3, all.size());
        assertEquals(10, all.get(0).id());
        assertEquals(20, all.get(1).id());
        assertEquals(30, all.get(2).id());
    }

    @Test
    @DisplayName("findAll() returns empty list when no departments exist")
    void findAll_empty() {
        assertTrue(dao.findAll().isEmpty());
    }

    // ── findByLocation() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByLocation() returns only departments in that location (case-insensitive)")
    void findByLocation_filtersCorrectly() {
        dao.add(engineering()); // Bangalore
        dao.add(hr());          // Mumbai
        dao.add(finance());     // Mumbai
        dao.add(sales());       // Delhi

        List<Department> mumbai = dao.findByLocation("mumbai");
        assertEquals(2, mumbai.size());
        assertTrue(mumbai.stream().allMatch(d -> d.location().equalsIgnoreCase("Mumbai")));
    }

    @Test
    @DisplayName("findByLocation() returns empty list when no match")
    void findByLocation_noMatch_returnsEmpty() {
        dao.add(engineering());
        assertTrue(dao.findByLocation("New York").isEmpty());
    }

    @Test
    @DisplayName("findByLocation() throws NullPointerException for null location")
    void findByLocation_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.findByLocation(null));
    }

    // ── count() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("count() returns 0 when no departments exist")
    void count_empty_returnsZero() {
        assertEquals(0, dao.count());
    }

    @Test
    @DisplayName("count() returns correct number of departments")
    void count_returnsCorrectCount() {
        dao.add(engineering());
        dao.add(hr());
        dao.add(finance());
        assertEquals(3, dao.count());
    }

    // ── constructor guard ─────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor throws NullPointerException when DataSourceFactory is null")
    void constructor_nullDsf_throws() {
        assertThrows(NullPointerException.class, () -> new JdbcDepartmentDao(null));
    }
}

