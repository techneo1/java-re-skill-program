package com.example.helloworld.db;

import com.example.helloworld.domain.PayrollRecord;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC-backed DAO for {@link PayrollRecord} persistence.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li><b>PreparedStatement</b> — parameterised INSERT and SELECT to prevent SQL injection</li>
 *   <li><b>ResultSet</b>         — iterating rows and reading typed columns</li>
 *   <li><b>java.sql.Date / Timestamp</b> — converting between {@code java.time} types
 *       and their JDBC counterparts via {@code Date.valueOf()} and {@code Timestamp.valueOf()}</li>
 * </ul>
 *
 * <p>Transaction management is intentionally kept <em>out</em> of this class so that a
 * caller (e.g. {@link PayrollTransactionService}) can enlist multiple DAO operations
 * inside one atomic transaction by sharing a single {@link Connection}.
 */
public class JdbcPayrollDao {

    // ── SQL constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO payroll_records " +
            "(id, employee_id, gross_salary, tax_amount, net_salary, payroll_month, processed_timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM payroll_records WHERE id=?";

    private static final String SQL_FIND_BY_EMPLOYEE =
            "SELECT * FROM payroll_records WHERE employee_id=? ORDER BY payroll_month DESC";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM payroll_records ORDER BY processed_timestamp DESC";

    private static final String SQL_DELETE_BY_EMPLOYEE =
            "DELETE FROM payroll_records WHERE employee_id=?";

    // ── State ─────────────────────────────────────────────────────────────────

    private final DataSourceFactory dsf;

    public JdbcPayrollDao(DataSourceFactory dsf) {
        this.dsf = Objects.requireNonNull(dsf, "DataSourceFactory must not be null");
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Persists a single {@link PayrollRecord}.
     *
     * <p>Parameter binding order mirrors the INSERT columns:
     * <pre>
     *   ps.setInt(1,       record.id())
     *   ps.setInt(2,       record.employeeId())
     *   ps.setDouble(3,    record.grossSalary())
     *   ps.setDouble(4,    record.taxAmount())
     *   ps.setDouble(5,    record.netSalary())
     *   ps.setDate(6,      Date.valueOf(record.payrollMonth()))
     *   ps.setTimestamp(7, Timestamp.valueOf(record.processedTimestamp()))
     * </pre>
     */
    public void save(PayrollRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {

            bindInsert(ps, record);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payroll record id=" + record.id(), e);
        }
    }

    /**
     * Persists a payroll record using a <em>caller-supplied</em> connection.
     * This overload is used by {@link PayrollTransactionService} so that
     * multiple saves can be wrapped in a single transaction.
     */
    public void save(Connection con, PayrollRecord record) throws SQLException {
        Objects.requireNonNull(con,    "connection must not be null");
        Objects.requireNonNull(record, "record must not be null");
        try (PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            bindInsert(ps, record);
            ps.executeUpdate();
        }
    }

    // ── Read operations ───────────────────────────────────────────────────────

    /**
     * Finds a payroll record by its primary key.
     *
     * <p>ResultSet navigation:
     * <pre>
     *   ResultSet rs = ps.executeQuery();
     *   if (rs.next()) { ... }   // advances cursor to first (only) row
     * </pre>
     */
    public Optional<PayrollRecord> findById(int id) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll record id=" + id, e);
        }
        return Optional.empty();
    }

    /**
     * Returns all payroll records for a given employee, newest first.
     *
     * <p>ResultSet iteration:
     * <pre>
     *   while (rs.next()) {          // iterate ALL rows
     *       results.add(mapRow(rs));
     *   }
     * </pre>
     */
    public List<PayrollRecord> findByEmployee(int employeeId) {
        List<PayrollRecord> results = new ArrayList<>();
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_EMPLOYEE)) {

            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll records for employee id=" + employeeId, e);
        }
        return Collections.unmodifiableList(results);
    }

    /** Returns every payroll record, most recently processed first. */
    public List<PayrollRecord> findAll() {
        List<PayrollRecord> results = new ArrayList<>();
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all payroll records", e);
        }
        return Collections.unmodifiableList(results);
    }

    /** Deletes all payroll records for the given employee (e.g. before removing the employee). */
    public int deleteByEmployee(int employeeId) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE_BY_EMPLOYEE)) {

            ps.setInt(1, employeeId);
            return ps.executeUpdate();   // returns number of rows deleted

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete payroll records for employee id=" + employeeId, e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Binds INSERT parameters for a {@link PayrollRecord}. */
    private void bindInsert(PreparedStatement ps, PayrollRecord r) throws SQLException {
        ps.setInt(1,       r.id());
        ps.setInt(2,       r.employeeId());
        ps.setDouble(3,    r.grossSalary());
        ps.setDouble(4,    r.taxAmount());
        ps.setDouble(5,    r.netSalary());
        // java.time → java.sql conversion
        ps.setDate(6,      Date.valueOf(r.payrollMonth()));
        ps.setTimestamp(7, Timestamp.valueOf(r.processedTimestamp()));
    }

    /**
     * Maps the current ResultSet row to a {@link PayrollRecord}.
     *
     * <p>Type conversions from JDBC to {@code java.time}:
     * <pre>
     *   rs.getDate("payroll_month").toLocalDate()
     *   rs.getTimestamp("processed_timestamp").toLocalDateTime()
     * </pre>
     */
    private PayrollRecord mapRow(ResultSet rs) throws SQLException {
        int           id          = rs.getInt("id");
        int           employeeId  = rs.getInt("employee_id");
        double        gross       = rs.getDouble("gross_salary");
        double        tax         = rs.getDouble("tax_amount");
        double        net         = rs.getDouble("net_salary");
        LocalDate     month       = rs.getDate("payroll_month").toLocalDate();
        LocalDateTime timestamp   = rs.getTimestamp("processed_timestamp").toLocalDateTime();
        return new PayrollRecord(id, employeeId, gross, tax, net, month, timestamp);
    }
}
