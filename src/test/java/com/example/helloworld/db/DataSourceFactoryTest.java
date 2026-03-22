package com.example.helloworld.db;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DataSourceFactory}.
 *
 * Covers:
 * - Obtaining a plain Connection (autoCommit=true by default)
 * - Obtaining a transactional Connection (autoCommit=false)
 * - Schema initialisation (DDL creates tables idempotently)
 * - Validation of blank URL
 */
@DisplayName("DataSourceFactory")
class DataSourceFactoryTest {

    private static final String URL  = "jdbc:h2:mem:dsf_test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private DataSourceFactory dsf;

    @BeforeEach
    void setUp() throws SQLException {
        dsf = new DataSourceFactory(URL, USER, PASS);
        dsf.initSchema();
    }

    // ── Connection ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getConnection() returns an open connection with autoCommit=true")
    void getConnection_returnsOpenConnection() throws SQLException {
        try (Connection con = dsf.getConnection()) {
            assertNotNull(con);
            assertFalse(con.isClosed());
            assertTrue(con.getAutoCommit(),
                    "Default connection should have autoCommit=true");
        }
    }

    @Test
    @DisplayName("getTransactionalConnection() returns a connection with autoCommit=false")
    void getTransactionalConnection_autoCommitDisabled() throws SQLException {
        try (Connection con = dsf.getTransactionalConnection()) {
            assertNotNull(con);
            assertFalse(con.isClosed());
            assertFalse(con.getAutoCommit(),
                    "Transactional connection should have autoCommit=false");
        }
    }

    @Test
    @DisplayName("connection can be closed and isClosed() reflects that")
    void connection_canBeClosed() throws SQLException {
        Connection con = dsf.getConnection();
        assertFalse(con.isClosed());
        con.close();
        assertTrue(con.isClosed());
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initSchema() is idempotent — calling twice does not throw")
    void initSchema_isIdempotent() {
        assertDoesNotThrow(() -> dsf.initSchema(),
                "Second call to initSchema() should not throw (IF NOT EXISTS)");
    }

    @Test
    @DisplayName("initSchema() creates the departments table")
    void initSchema_createsDepartmentsTable() throws SQLException {
        try (Connection con = dsf.getConnection();
             var ps = con.prepareStatement("SELECT COUNT(*) FROM departments");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());   // table exists → query succeeds
        }
    }

    @Test
    @DisplayName("initSchema() creates the employees table")
    void initSchema_createsEmployeesTable() throws SQLException {
        try (Connection con = dsf.getConnection();
             var ps = con.prepareStatement("SELECT COUNT(*) FROM employees");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());   // table exists → query succeeds
        }
    }

    @Test
    @DisplayName("initSchema() creates the payroll_records table")
    void initSchema_createsPayrollRecordsTable() throws SQLException {
        try (Connection con = dsf.getConnection();
             var ps = con.prepareStatement("SELECT COUNT(*) FROM payroll_records");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("blank URL throws IllegalArgumentException")
    void constructor_blankUrl_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DataSourceFactory("  ", USER, PASS));
    }

    @Test
    @DisplayName("null URL throws IllegalArgumentException")
    void constructor_nullUrl_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DataSourceFactory(null, USER, PASS));
    }

    @Test
    @DisplayName("getUrl() returns the configured URL")
    void getUrl_returnsConfiguredUrl() {
        assertEquals(URL, dsf.getUrl());
    }
}
