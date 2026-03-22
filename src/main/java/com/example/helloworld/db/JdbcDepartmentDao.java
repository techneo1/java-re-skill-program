package com.example.helloworld.db;

import com.example.helloworld.domain.Department;
import com.example.helloworld.exception.DepartmentNotFoundException;
import com.example.helloworld.repository.DepartmentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link DepartmentRepository}.
 *
 * <p>Key JDBC concepts demonstrated:
 * <ul>
 *   <li><b>PreparedStatement</b> — used for all DML/queries to prevent SQL injection.</li>
 *   <li><b>ResultSet</b>         — columns read by name via typed getters.</li>
 *   <li><b>try-with-resources</b> — Connection, PreparedStatement and ResultSet are
 *       all auto-closed to prevent resource leaks.</li>
 * </ul>
 */
public class JdbcDepartmentDao implements DepartmentRepository {

    // ── SQL constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO departments (id, name, location) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE departments SET name=?, location=? WHERE id=?";

    private static final String SQL_DELETE =
            "DELETE FROM departments WHERE id=?";

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM departments WHERE id=?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT * FROM departments WHERE LOWER(name)=LOWER(?)";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM departments ORDER BY id";

    private static final String SQL_FIND_BY_LOCATION =
            "SELECT * FROM departments WHERE LOWER(location)=LOWER(?) ORDER BY id";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM departments";

    // ── State ─────────────────────────────────────────────────────────────────

    private final DataSourceFactory dsf;

    public JdbcDepartmentDao(DataSourceFactory dsf) {
        this.dsf = Objects.requireNonNull(dsf, "DataSourceFactory must not be null");
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Inserts a new department row.
     *
     * <p>PreparedStatement usage:
     * <pre>
     *   con.prepareStatement(SQL_INSERT)
     *   ps.setInt(1, department.id())      // positional parameter binding
     *   ps.executeUpdate()                 // returns rows affected
     * </pre>
     */
    @Override
    public void add(Department department) {
        Objects.requireNonNull(department, "department must not be null");

        if (findById(department.id()).isPresent())
            throw new IllegalArgumentException(
                    "Department with id " + department.id() + " already exists");

        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {

            ps.setInt(1,    department.id());
            ps.setString(2, department.name());
            ps.setString(3, department.location());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert department id=" + department.id(), e);
        }
    }

    /**
     * Updates the name and location of an existing department.
     *
     * @throws DepartmentNotFoundException if no row is matched by id
     */
    @Override
    public void update(Department department) throws DepartmentNotFoundException {
        Objects.requireNonNull(department, "department must not be null");

        if (findById(department.id()).isEmpty())
            throw new DepartmentNotFoundException(department.id());

        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, department.name());
            ps.setString(2, department.location());
            ps.setInt(3,    department.id());

            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new DepartmentNotFoundException(department.id());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update department id=" + department.id(), e);
        }
    }

    /**
     * Deletes a department row by primary key.
     *
     * @throws DepartmentNotFoundException if no row is matched
     */
    @Override
    public void remove(int id) throws DepartmentNotFoundException {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new DepartmentNotFoundException(id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove department id=" + id, e);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Looks up a single department by primary key.
     *
     * <p>ResultSet usage:
     * <pre>
     *   ResultSet rs = ps.executeQuery()
     *   if (rs.next()) { ... }    // false = no row
     *   rs.getInt("id")           // read column by name
     * </pre>
     */
    @Override
    public Optional<Department> findById(int id) {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find department id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Department> findByName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find department by name=" + name, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Department> findAll() {
        return executeListQuery(SQL_FIND_ALL, ps -> {});
    }

    @Override
    public List<Department> findByLocation(String location) {
        Objects.requireNonNull(location, "location must not be null");
        return executeListQuery(SQL_FIND_BY_LOCATION, ps -> ps.setString(1, location));
    }

    @Override
    public int count() {
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_COUNT);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count departments", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ParamBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Department> executeListQuery(String sql, ParamBinder binder) {
        List<Department> results = new ArrayList<>();
        try (Connection con = dsf.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Maps the current ResultSet row to a {@link Department} record.
     *
     * <pre>
     *   rs.getInt("id")
     *   rs.getString("name")
     *   rs.getString("location")
     * </pre>
     */
    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("location")
        );
    }
}

