package com.example.helloworld.db;

import com.example.helloworld.domain.*;
import com.example.helloworld.db.PayrollTransactionService.PayrollTransactionException;
import com.example.helloworld.exception.PayrollException;
import com.example.helloworld.service.PayrollService;
import com.example.helloworld.service.PayrollServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link PayrollTransactionService}.
 *
 * Demonstrates JDBC transaction concepts:
 * - Successful batch → commit → all records persisted
 * - Calculation failure mid-batch → rollback → zero records persisted
 * - Single employee happy path
 * - Single employee failure → rollback
 *
 * A stub PayrollService is used to simulate calculation failures
 * without depending on the real strategy internals.
 */
@DisplayName("PayrollTransactionService")
class PayrollTransactionServiceTest {

    private static final String URL  = "jdbc:h2:mem:txn_test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private static final LocalDate MONTH = LocalDate.of(2024, 3, 1);

    private DataSourceFactory        dsf;
    private JdbcEmployeeDao          empDao;
    private JdbcPayrollDao           payrollDao;
    private PayrollTransactionService txService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee alice() {
        return new PermanentEmployee(1, "Alice", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    private static PermanentEmployee bob() {
        return new PermanentEmployee(2, "Bob", "bob@example.com",
                10, "Manager", 120_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), false);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        dsf = new DataSourceFactory(URL, USER, PASS);
        dsf.initSchema();
        empDao     = new JdbcEmployeeDao(dsf);
        payrollDao = new JdbcPayrollDao(dsf);

        // wipe tables for test isolation
        try (var con = dsf.getConnection(); var stmt = con.createStatement()) {
            stmt.execute("DELETE FROM payroll_records");
            stmt.execute("DELETE FROM employees");
        }

        empDao.add(alice());
        empDao.add(bob());

        // Default: use real PayrollServiceImpl
        txService = new PayrollTransactionService(dsf, new PayrollServiceImpl(), payrollDao);
    }

    // ── processAndSaveAll() ───────────────────────────────────────────────────

    @Test
    @DisplayName("processAndSaveAll() commits all records when every employee succeeds")
    void processAndSaveAll_success_commitsAll() throws Exception {
        List<Employee> employees = List.of(alice(), bob());
        List<PayrollRecord> records = txService.processAndSaveAll(employees, MONTH);

        assertEquals(2, records.size());
        // Verify DB actually has the rows (not just in-memory)
        assertEquals(2, payrollDao.findAll().size());
        assertEquals(1, payrollDao.findByEmployee(1).size());
        assertEquals(1, payrollDao.findByEmployee(2).size());
    }

    @Test
    @DisplayName("processAndSaveAll() rolls back ALL records when one calculation fails")
    void processAndSaveAll_calcFailure_rollsBackAll() {
        // Stub service that throws for employee id=2
        PayrollService failingService = new PayrollService() {
            @Override
            public PayrollRecord processPayroll(int recordId, Employee employee, LocalDate month)
                    throws PayrollException {
                if (employee.getId() == 2)
                    throw new PayrollException(employee.getId(), "Simulated failure for id=2");
                return new PayrollServiceImpl().processPayroll(recordId, employee, month);
            }

            @Override
            public List<PayrollRecord> processAll(List<Employee> employees, LocalDate month) {
                return List.of();
            }
        };

        PayrollTransactionService svc =
                new PayrollTransactionService(dsf, failingService, payrollDao);

        List<Employee> employees = List.of(alice(), bob());

        // Should throw because id=2 fails
        assertThrows(PayrollTransactionException.class,
                () -> svc.processAndSaveAll(employees, MONTH));

        // Rollback: NO records should be in DB
        assertTrue(payrollDao.findAll().isEmpty(),
                "Rollback should have removed all staged records");
    }

    @Test
    @DisplayName("processAndSaveAll() with empty list commits with zero records")
    void processAndSaveAll_emptyList_returnsEmpty() throws Exception {
        List<PayrollRecord> records = txService.processAndSaveAll(List.of(), MONTH);
        assertTrue(records.isEmpty());
        assertTrue(payrollDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("processAndSaveAll() throws NullPointerException for null employees")
    void processAndSaveAll_nullEmployees_throws() {
        assertThrows(NullPointerException.class,
                () -> txService.processAndSaveAll(null, MONTH));
    }

    @Test
    @DisplayName("processAndSaveAll() throws NullPointerException for null month")
    void processAndSaveAll_nullMonth_throws() {
        assertThrows(NullPointerException.class,
                () -> txService.processAndSaveAll(List.of(alice()), null));
    }

    // ── processAndSave() (single employee) ───────────────────────────────────

    @Test
    @DisplayName("processAndSave() commits a single payroll record")
    void processAndSave_success_commitsRecord() throws Exception {
        PayrollRecord record = txService.processAndSave(1, alice(), MONTH);
        assertNotNull(record);
        assertTrue(payrollDao.findById(record.id()).isPresent(),
                "Record should be persisted after commit");
    }

    @Test
    @DisplayName("processAndSave() rolls back when calculation fails")
    void processAndSave_failure_rollsBack() {
        PayrollService failingService = new PayrollService() {
            @Override
            public PayrollRecord processPayroll(int id, Employee e, LocalDate m)
                    throws PayrollException {
                throw new PayrollException(e.getId(), "Deliberate failure");
            }

            @Override
            public List<PayrollRecord> processAll(List<Employee> employees, LocalDate month) {
                return List.of();
            }
        };

        PayrollTransactionService svc =
                new PayrollTransactionService(dsf, failingService, payrollDao);

        assertThrows(PayrollTransactionException.class,
                () -> svc.processAndSave(1, alice(), MONTH));

        assertTrue(payrollDao.findAll().isEmpty(),
                "Rollback should leave no records");
    }

    // ── Constructor guards ───────────────────────────────��────────────────────

    @Test
    @DisplayName("constructor throws NullPointerException when DataSourceFactory is null")
    void constructor_nullDsf_throws() {
        assertThrows(NullPointerException.class,
                () -> new PayrollTransactionService(null, new PayrollServiceImpl(), payrollDao));
    }

    @Test
    @DisplayName("constructor throws NullPointerException when PayrollService is null")
    void constructor_nullPayrollService_throws() {
        assertThrows(NullPointerException.class,
                () -> new PayrollTransactionService(dsf, null, payrollDao));
    }

    @Test
    @DisplayName("constructor throws NullPointerException when JdbcPayrollDao is null")
    void constructor_nullPayrollDao_throws() {
        assertThrows(NullPointerException.class,
                () -> new PayrollTransactionService(dsf, new PayrollServiceImpl(), null));
    }
}
