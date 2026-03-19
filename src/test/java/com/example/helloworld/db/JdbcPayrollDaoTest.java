package com.example.helloworld.db;

import com.example.helloworld.domain.*;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link JdbcPayrollDao}.
 *
 * Covers:
 * - PreparedStatement INSERT (via {@code save()})
 * - ResultSet mapping — all fields including java.sql.Date → LocalDate
 *   and java.sql.Timestamp → LocalDateTime
 * - findById, findByEmployee, findAll
 * - deleteByEmployee
 * - Connection-sharing overload of save() (transactional usage)
 */
@DisplayName("JdbcPayrollDao")
class JdbcPayrollDaoTest {

    private static final String URL  = "jdbc:h2:mem:payroll_dao_test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private DataSourceFactory dsf;
    private JdbcPayrollDao    dao;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final LocalDate     MONTH     = LocalDate.of(2024, 3, 1);
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2024, 3, 31, 10, 0, 0);

    private static PermanentEmployee alice() {
        return new PermanentEmployee(1, "Alice", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    private static PayrollRecord record1() {
        return new PayrollRecord(1, 1, 85_000, 17_000, 68_000, MONTH, TIMESTAMP);
    }

    private static PayrollRecord record2() {
        return new PayrollRecord(2, 1, 85_000, 17_000, 68_000,
                LocalDate.of(2024, 2, 1), TIMESTAMP.minusMonths(1));
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        dsf = new DataSourceFactory(URL, USER, PASS);
        dsf.initSchema();
        dao = new JdbcPayrollDao(dsf);
        try (var con = dsf.getConnection(); var stmt = con.createStatement()) {
            stmt.execute("DELETE FROM payroll_records");
            stmt.execute("DELETE FROM employees");
        }
        // insert alice so FK constraint is satisfied
        new JdbcEmployeeDao(dsf).add(alice());
    }

    // ── save() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() persists a PayrollRecord — findById retrieves it")
    void save_persistsRecord() {
        dao.save(record1());
        Optional<PayrollRecord> found = dao.findById(1);
        assertTrue(found.isPresent());
        PayrollRecord r = found.get();
        assertEquals(1,       r.id());
        assertEquals(1,       r.employeeId());
        assertEquals(85_000,  r.grossSalary(), 0.01);
        assertEquals(17_000,  r.taxAmount(),   0.01);
        assertEquals(68_000,  r.netSalary(),   0.01);
        assertEquals(MONTH,   r.payrollMonth());
        assertEquals(TIMESTAMP, r.processedTimestamp());
    }

    @Test
    @DisplayName("save(Connection, record) uses caller-supplied connection (transactional overload)")
    void save_withConnection_usesSuppliedConnection() throws Exception {
        try (var con = dsf.getTransactionalConnection()) {
            dao.save(con, record1());
            con.commit();
        }
        assertTrue(dao.findById(1).isPresent());
    }

    @Test
    @DisplayName("save(Connection, record) rollback discards the record")
    void save_withConnection_rollbackDiscardsRecord() throws Exception {
        try (var con = dsf.getTransactionalConnection()) {
            dao.save(con, record1());
            con.rollback();   // ← explicit rollback
        }
        assertTrue(dao.findById(1).isEmpty(),
                "Record should not be visible after rollback");
    }

    @Test
    @DisplayName("save() throws NullPointerException for null record")
    void save_null_throws() {
        assertThrows(NullPointerException.class, () -> dao.save(null));
    }

    // ── findById() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() returns empty for unknown id")
    void findById_notFound_returnsEmpty() {
        assertTrue(dao.findById(999).isEmpty());
    }

    // ── findByEmployee() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmployee() returns records for that employee, newest first")
    void findByEmployee_returnsMatchingRecords() {
        dao.save(record1());  // March 2024
        dao.save(record2());  // Feb 2024
        List<PayrollRecord> records = dao.findByEmployee(1);
        assertEquals(2, records.size());
        // ORDER BY payroll_month DESC → March first
        assertEquals(MONTH, records.get(0).payrollMonth());
    }

    @Test
    @DisplayName("findByEmployee() returns empty list for employee with no records")
    void findByEmployee_noRecords_returnsEmpty() {
        assertTrue(dao.findByEmployee(1).isEmpty());
    }

    // ── findAll() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() returns all payroll records")
    void findAll_returnsAll() {
        dao.save(record1());
        dao.save(record2());
        assertEquals(2, dao.findAll().size());
    }

    @Test
    @DisplayName("findAll() returns empty list when no records")
    void findAll_empty() {
        assertTrue(dao.findAll().isEmpty());
    }

    // ── deleteByEmployee() ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteByEmployee() removes all records for the employee")
    void deleteByEmployee_removesAll() {
        dao.save(record1());
        dao.save(record2());
        int deleted = dao.deleteByEmployee(1);
        assertEquals(2, deleted);
        assertTrue(dao.findByEmployee(1).isEmpty());
    }

    @Test
    @DisplayName("deleteByEmployee() returns 0 when no records exist for employee")
    void deleteByEmployee_noRecords_returnsZero() {
        assertEquals(0, dao.deleteByEmployee(999));
    }
}
