package com.example.helloworld.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC Connection creation.
 *
 * <p>Demonstrates the JDBC Connection API:
 * <ul>
 *   <li>{@link DriverManager#getConnection} — obtain a physical connection</li>
 *   <li>{@link Connection#setAutoCommit} — control transaction boundaries</li>
 *   <li>{@link Connection#close}         — release the connection back to the driver</li>
 * </ul>
 *
 * <p>In production you would replace {@link DriverManager} with a proper
 * connection pool (HikariCP, c3p0, etc.).  The interface here keeps that swap
 * trivial: just provide a different {@code DataSourceFactory} implementation.
 */
public class DataSourceFactory {

    private final String url;
    private final String username;
    private final String password;

    /**
     * Creates a factory that opens connections to the given JDBC URL.
     *
     * @param url      JDBC connection URL  (e.g. {@code jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1})
     * @param username database username
     * @param password database password
     */
    public DataSourceFactory(String url, String username, String password) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("JDBC url must not be blank");
        this.url      = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Opens and returns a new {@link Connection}.
     *
     * <p>Auto-commit is <em>enabled</em> by default — each statement is its own
     * transaction.  Call {@code con.setAutoCommit(false)} before starting a
     * multi-statement transaction.
     *
     * @return a fresh JDBC {@link Connection}
     * @throws SQLException if the driver cannot establish a connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Opens a connection with auto-commit <em>disabled</em>, ready for an
     * explicit transaction (commit / rollback).
     *
     * @return a {@link Connection} with {@code autoCommit = false}
     * @throws SQLException if the driver cannot establish a connection
     */
    public Connection getTransactionalConnection() throws SQLException {
        Connection con = getConnection();
        con.setAutoCommit(false);   // ← manual transaction control
        return con;
    }

    // ── Schema bootstrap (DDL) ────────────────────────────────────────────────

    /**
     * Creates the {@code employees} and {@code payroll_records} tables if they
     * do not already exist.  Uses a plain {@link java.sql.Statement} for DDL —
     * no user input, so no injection risk.
     *
     * @throws SQLException if the DDL cannot be executed
     */
    public void initSchema() throws SQLException {
        try (Connection con = getConnection();
             var stmt = con.createStatement()) {

            // departments must be created first — employees references it via FK
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    id       INT          PRIMARY KEY,
                    name     VARCHAR(100) NOT NULL UNIQUE,
                    location VARCHAR(150) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    id               INT          PRIMARY KEY,
                    name             VARCHAR(100) NOT NULL,
                    email            VARCHAR(150) NOT NULL UNIQUE,
                    department_id    INT          NOT NULL,
                    role             VARCHAR(80)  NOT NULL,
                    salary           DOUBLE       NOT NULL,
                    status           VARCHAR(20)  NOT NULL,
                    joining_date     DATE         NOT NULL,
                    employee_type    VARCHAR(20)  NOT NULL,
                    gratuity_eligible BOOLEAN     DEFAULT FALSE,
                    contract_end_date DATE        DEFAULT NULL,
                    FOREIGN KEY (department_id) REFERENCES departments(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payroll_records (
                    id                  INT          PRIMARY KEY,
                    employee_id         INT          NOT NULL,
                    gross_salary        DOUBLE       NOT NULL,
                    tax_amount          DOUBLE       NOT NULL,
                    net_salary          DOUBLE       NOT NULL,
                    payroll_month       DATE         NOT NULL,
                    processed_timestamp TIMESTAMP    NOT NULL,
                    FOREIGN KEY (employee_id) REFERENCES employees(id)
                )
            """);
        }
    }

    // ── Getters (useful for diagnostics / logging) ────────────────────────────

    public String getUrl() { return url; }
}
