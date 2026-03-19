package com.example.helloworld.db;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.PayrollRecord;
import com.example.helloworld.exception.PayrollException;
import com.example.helloworld.service.PayrollService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Demonstrates JDBC <b>transaction management</b>.
 *
 * <p>A <em>transaction</em> groups multiple SQL statements so they succeed or
 * fail atomically.  The JDBC recipe is:
 * <pre>
 *   con.setAutoCommit(false);  // 1. disable auto-commit  → start transaction
 *   // ... execute statements
 *   con.commit();              // 2. commit   → all changes become permanent
 *   // OR
 *   con.rollback();            // 3. rollback → all changes are discarded
 *   con.setAutoCommit(true);   // 4. restore auto-commit (optional but tidy)
 * </pre>
 *
 * <p>Use-case: processing payroll for a batch of employees.
 * <ul>
 *   <li>All payroll records are written atomically — either every record is
 *       saved, or none are (if any single save fails).</li>
 *   <li>Contrast with {@link com.example.helloworld.service.PayrollServiceImpl#processAll}
 *       which is fault-tolerant (skip-on-error); this class is <em>all-or-nothing</em>.</li>
 * </ul>
 */
public class PayrollTransactionService {

    private final DataSourceFactory dsf;
    private final PayrollService    payrollService;
    private final JdbcPayrollDao    payrollDao;

    public PayrollTransactionService(DataSourceFactory dsf,
                                     PayrollService payrollService,
                                     JdbcPayrollDao payrollDao) {
        this.dsf            = Objects.requireNonNull(dsf,            "DataSourceFactory must not be null");
        this.payrollService = Objects.requireNonNull(payrollService, "PayrollService must not be null");
        this.payrollDao     = Objects.requireNonNull(payrollDao,     "JdbcPayrollDao must not be null");
    }

    /**
     * Processes payroll for every employee and persists all records in a
     * <b>single atomic transaction</b>.
     *
     * <p>If any employee fails (calculation error or DB error) the entire
     * batch is rolled back and a {@link PayrollTransactionException} is thrown.
     *
     * <p>JDBC transaction flow:
     * <ol>
     *   <li>{@code con.setAutoCommit(false)} — start the transaction</li>
     *   <li>Loop: calculate + {@code payrollDao.save(con, record)}</li>
     *   <li>{@code con.commit()} — make all inserts permanent</li>
     *   <li>On error: {@code con.rollback()} — undo every insert in this batch</li>
     * </ol>
     *
     * @param employees    the employees to pay
     * @param payrollMonth the month for which payroll is processed
     * @return immutable list of persisted {@link PayrollRecord}s
     * @throws PayrollTransactionException if any employee fails or the DB write fails
     */
    public List<PayrollRecord> processAndSaveAll(List<Employee> employees,
                                                  LocalDate payrollMonth)
            throws PayrollTransactionException {

        Objects.requireNonNull(employees,    "employees must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");

        List<PayrollRecord> records = new ArrayList<>();

        // ── Step 1: obtain a transactional connection ─────────────────────────
        try (Connection con = dsf.getTransactionalConnection()) {   // autoCommit=false
            try {
                int recordId = 1;

                // ── Step 2: calculate + stage each INSERT inside the transaction ───
                for (Employee emp : employees) {
                    PayrollRecord record;
                    try {
                        record = payrollService.processPayroll(recordId++, emp, payrollMonth);
                    } catch (PayrollException e) {
                        // Calculation failed → roll back everything already staged
                        con.rollback();  // ← ROLLBACK
                        throw new PayrollTransactionException(
                                "Payroll calculation failed for employee id=" + emp.getId()
                                + ": " + e.getMessage(), e);
                    }

                    // Uses the shared connection so the INSERT is part of the transaction
                    payrollDao.save(con, record);
                    records.add(record);
                }

                // ── Step 3: all succeeded → commit ────────────────────────────────
                con.commit();    // ← COMMIT
                return List.copyOf(records);

            } catch (PayrollTransactionException pte) {
                throw pte;   // already rolled back above; re-throw as-is

            } catch (SQLException e) {
                // DB write failed → roll back
                safeRollback(con);
                throw new PayrollTransactionException(
                        "Database error during payroll batch — transaction rolled back", e);
            }

        } catch (SQLException e) {
            throw new PayrollTransactionException(
                    "Could not obtain a database connection", e);
        }
    }

    /**
     * Processes and saves payroll for a <b>single employee</b> inside its own
     * transaction.  Demonstrates the simplest possible commit/rollback pattern.
     *
     * @return the persisted {@link PayrollRecord}
     * @throws PayrollTransactionException if calculation or persistence fails
     */
    public PayrollRecord processAndSave(int recordId, Employee employee, LocalDate payrollMonth)
            throws PayrollTransactionException {

        Objects.requireNonNull(employee,     "employee must not be null");
        Objects.requireNonNull(payrollMonth, "payrollMonth must not be null");

        try (Connection con = dsf.getTransactionalConnection()) {
            try {
                PayrollRecord record = payrollService.processPayroll(recordId, employee, payrollMonth);
                payrollDao.save(con, record);
                con.commit();         // ← COMMIT
                return record;

            } catch (PayrollException | SQLException e) {
                safeRollback(con);    // ← ROLLBACK on any error
                throw new PayrollTransactionException(
                        "Failed to process/save payroll for employee id=" + employee.getId(), e);
            }
        } catch (SQLException e) {
            throw new PayrollTransactionException("Could not obtain a database connection", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Rolls back quietly — logs but does not mask the original exception. */
    private void safeRollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ex) {
            System.err.println("[PayrollTransactionService] rollback failed: " + ex.getMessage());
        }
    }

    // ── Nested exception ──────────────────────────────────────────────────────

    /**
     * Thrown when a transactional payroll batch fails (calculation or DB error).
     * The transaction has already been rolled back when this exception is raised.
     */
    public static final class PayrollTransactionException extends Exception {
        public PayrollTransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

