package com.example.helloworld.db;

import com.example.helloworld.domain.*;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.EmployeeRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

/**
 * JDBC-backed implementation of {@link EmployeeRepository}.
 *
 * <p>Key JDBC concepts demonstrated:
 * <ul>
 *   <li><b>Connection</b>  — obtained from {@link DataSourceFactory} for every operation
 *       and closed in a try-with-resources block.</li>
 *   <li><b>PreparedStatement</b> — used for every DML/query to prevent SQL injection
 *       and improve readability.  Parameters are bound by index with typed setters
 *       (setInt, setString, setDouble, setDate).</li>
 *   <li><b>ResultSet</b>  — iterated with {@code rs.next()} and columns read by name
 *       via typed getters (getInt, getString, getDouble, getDate).</li>
 *   <li><b>Generated Keys</b> — {@code RETURN_GENERATED_KEYS} flag shows how to
 *       retrieve auto-generated primary keys (not used here because the domain
 *       model owns its own IDs, but the pattern is shown in a dedicated method).</li>
 * </ul>
 */
public class JdbcEmployeeDao implements EmployeeRepository {

    // ── SQL constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO employees (id, name, email, department_id, role, salary, " +
            "status, joining_date, employee_type, gratuity_eligible, contract_end_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE employees SET name=?, email=?, department_id=?, role=?, salary=?, " +
            "status=?, joining_date=?, gratuity_eligible=?, contract_end_date=? WHERE id=?";

    private static final String SQL_DELETE       = "DELETE FROM employees WHERE id=?";
    private static final String SQL_FIND_BY_ID   = "SELECT * FROM employees WHERE id=?";
    private static final String SQL_FIND_ALL     = "SELECT * FROM employees";
    private static final String SQL_FIND_BY_DEPT = "SELECT * FROM employees WHERE department_id=?";
    private static final String SQL_FIND_BY_STATUS = "SELECT * FROM employees WHERE status=?";
    private static final String SQL_FIND_BY_ROLE   = "SELECT * FROM employees WHERE role=?";
    private static final String SQL_FIND_BY_EMAIL  = "SELECT * FROM employees WHERE LOWER(email)=LOWER(?)";
    private static final String SQL_FIND_BY_SALARY =
            "SELECT * FROM employees WHERE salary >= ? AND salary <= ?";
    private static final String SQL_COUNT        = "SELECT COUNT(*) FROM employees";
    private static final String SQL_TOTAL_SALARY = "SELECT SUM(salary) FROM employees";
    private static final String SQL_AVG_SALARY   = "SELECT AVG(salary) FROM employees";

    // ── State ─────────────────────────────────────────────────────────────────

    private final DataSourceFactory dsf;

    public JdbcEmployeeDao(DataSourceFactory dsf) {
        this.dsf = Objects.requireNonNull(dsf, "DataSourceFactory must not be null");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Inserts a new employee row.
     *
     * <p>PreparedStatement usage:
     * <pre>
     *   con.prepareStatement(SQL_INSERT)   // compile once, bind params below
     *   ps.setInt(1, employee.getId())     // positional parameter binding
     *   ps.executeUpdate()                 // returns rows affected
     * </pre>
     */
    @Override
    public void add(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");

        // Check for duplicate ID
        if (findById(employee.getId()).isPresent())
            throw new DuplicateEmployeeException(employee.getId());

        // Check for duplicate e-mail
        if (findByEmail(employee.getEmail()).isPresent())
            throw new DuplicateEmailException(employee.getEmail());

        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {

            bindInsertParams(ps, employee);
            ps.executeUpdate();   // ← executes the INSERT

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert employee id=" + employee.getId(), e);
        }
    }

    /**
     * Updates an existing employee row.
     *
     * <p>If {@code executeUpdate()} returns 0 no row was matched — employee not found.
     */
    @Override
    public void update(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");

        // Verify it exists first
        if (findById(employee.getId()).isEmpty())
            throw new EmployeeNotFoundException(employee.getId());

        // Check e-mail uniqueness against *other* employees
        Optional<Employee> byEmail = findByEmail(employee.getEmail());
        if (byEmail.isPresent() && byEmail.get().getId() != employee.getId())
            throw new DuplicateEmailException(employee.getEmail());

        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, employee.getName());
            ps.setString(2, employee.getEmail());
            ps.setInt(3,    employee.getDepartmentId());
            ps.setString(4, employee.getRole());
            ps.setDouble(5, employee.getSalary());
            ps.setString(6, employee.getStatus().name());
            ps.setDate(7,   Date.valueOf(employee.getJoiningDate()));
            // type-specific extra columns
            if (employee instanceof PermanentEmployee pe) {
                ps.setBoolean(8, pe.isGratuityEligible());
                ps.setNull(9, Types.DATE);
            } else if (employee instanceof ContractEmployee ce) {
                ps.setBoolean(8, false);
                ps.setDate(9, Date.valueOf(ce.getContractEndDate()));
            }
            ps.setInt(10, employee.getId());

            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new EmployeeNotFoundException(employee.getId());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee id=" + employee.getId(), e);
        }
    }

    /**
     * Deletes a row by primary key.
     */
    @Override
    public void remove(int id) throws EmployeeNotFoundException {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);                   // bind parameter
            int rows = ps.executeUpdate();      // execute DELETE
            if (rows == 0)
                throw new EmployeeNotFoundException(id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove employee id=" + id, e);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Looks up a single employee by primary key.
     *
     * <p>ResultSet usage:
     * <pre>
     *   ResultSet rs = ps.executeQuery()   // execute SELECT
     *   if (rs.next()) { ... }             // advance cursor; false = no row
     *   rs.getInt("id")                    // read column by name
     * </pre>
     */
    @Override
    public Optional<Employee> findById(int id) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {   // ← executeQuery returns ResultSet
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employee id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findAll() {
        return executeListQuery(SQL_FIND_ALL, ps -> {});
    }

    @Override
    public List<Employee> findByDepartment(int departmentId) {
        return executeListQuery(SQL_FIND_BY_DEPT, ps -> ps.setInt(1, departmentId));
    }

    @Override
    public List<Employee> findByStatus(EmployeeStatus status) {
        return executeListQuery(SQL_FIND_BY_STATUS, ps -> ps.setString(1, status.name()));
    }

    @Override
    public List<Employee> findByRole(String role) {
        return executeListQuery(SQL_FIND_BY_ROLE, ps -> ps.setString(1, role));
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employee by email=" + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findBySalaryRange(double min, double max)
            throws InvalidEmployeeDataException {
        if (min < 0 || max < min)
            throw new InvalidEmployeeDataException("salaryRange",
                    min + "-" + max, "min must be >= 0 and <= max");
        return executeListQuery(SQL_FIND_BY_SALARY, ps -> {
            ps.setDouble(1, min);
            ps.setDouble(2, max);
        });
    }

    // ── Aggregate queries ─────────────────────────────────────────────────────

    @Override
    public int count() {
        return executeScalarInt(SQL_COUNT);
    }

    @Override
    public double totalSalary() {
        return executeScalarDouble(SQL_TOTAL_SALARY);
    }

    @Override
    public double averageSalary() {
        return executeScalarDouble(SQL_AVG_SALARY);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Functional interface that allows checked SQLException. */
    @FunctionalInterface
    private interface ParamBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /**
     * Generic helper: prepares a statement, binds parameters via {@code binder},
     * executes the query, and maps every row via {@link #mapRow(ResultSet)}.
     */
    private List<Employee> executeListQuery(String sql, ParamBinder binder) {
        List<Employee> results = new ArrayList<>();
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {   // iterate ResultSet
                while (rs.next()) {                    // rs.next() advances the cursor
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return Collections.unmodifiableList(results);
    }

    private int executeScalarInt(String sql) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Scalar int query failed: " + sql, e);
        }
    }

    private double executeScalarDouble(String sql) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Scalar double query failed: " + sql, e);
        }
    }

    /**
     * Maps the current row of a {@link ResultSet} to an {@link Employee} domain object.
     *
     * <p>Column values are read by <em>name</em> for clarity:
     * <pre>
     *   rs.getInt("id")
     *   rs.getString("employee_type")
     *   rs.getDate("joining_date").toLocalDate()
     * </pre>
     */
    private Employee mapRow(ResultSet rs) throws SQLException {
        int    id           = rs.getInt("id");
        String name         = rs.getString("name");
        String email        = rs.getString("email");
        int    deptId       = rs.getInt("department_id");
        String role         = rs.getString("role");
        double salary       = rs.getDouble("salary");
        String statusStr    = rs.getString("status");
        LocalDate joinDate  = rs.getDate("joining_date").toLocalDate();
        String empType      = rs.getString("employee_type");

        EmployeeStatus status = EmployeeStatus.valueOf(statusStr);

        return switch (empType) {
            case "PERMANENT" -> {
                boolean gratuity = rs.getBoolean("gratuity_eligible");
                yield new PermanentEmployee(id, name, email, deptId,
                        role, salary, status, joinDate, gratuity);
            }
            case "CONTRACT" -> {
                Date contractEndRaw = rs.getDate("contract_end_date");
                LocalDate contractEnd = contractEndRaw != null
                        ? contractEndRaw.toLocalDate() : null;
                yield new ContractEmployee(id, name, email, deptId,
                        role, salary, status, joinDate, contractEnd);
            }
            default -> throw new IllegalStateException("Unknown employee_type: " + empType);
        };
    }

    /**
     * Binds all 11 INSERT parameters for any {@link Employee} subtype.
     */
    private void bindInsertParams(PreparedStatement ps, Employee emp) throws SQLException {
        ps.setInt(1,    emp.getId());
        ps.setString(2, emp.getName());
        ps.setString(3, emp.getEmail());
        ps.setInt(4,    emp.getDepartmentId());
        ps.setString(5, emp.getRole());
        ps.setDouble(6, emp.getSalary());
        ps.setString(7, emp.getStatus().name());
        ps.setDate(8,   Date.valueOf(emp.getJoiningDate()));

        if (emp instanceof PermanentEmployee pe) {
            ps.setString(9,  "PERMANENT");
            ps.setBoolean(10, pe.isGratuityEligible());
            ps.setNull(11, Types.DATE);
        } else if (emp instanceof ContractEmployee ce) {
            ps.setString(9, "CONTRACT");
            ps.setBoolean(10, false);
            ps.setDate(11, Date.valueOf(ce.getContractEndDate()));
        }
    }
}
