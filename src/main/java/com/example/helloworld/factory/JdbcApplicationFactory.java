package com.example.helloworld.factory;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.db.DataSourceFactory;
import com.example.helloworld.db.JdbcEmployeeDao;
import com.example.helloworld.repository.EmployeeRepository;
import com.example.helloworld.service.*;

import java.sql.SQLException;

/**
 * Concrete {@link ApplicationFactory} that wires the full application stack
 * using a JDBC-backed repository ({@link JdbcEmployeeDao}).
 *
 * <p>Drop-in replacement for {@link InMemoryApplicationFactory}: no other
 * code needs to change — just swap the factory at the composition root.
 *
 * <pre>{@code
 *   ApplicationFactory factory = new JdbcApplicationFactory(
 *       "jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1", "sa", "");
 *   EmployeeController ctrl = factory.createEmployeeController();
 * }</pre>
 */
public class JdbcApplicationFactory implements ApplicationFactory {

    private final DataSourceFactory dsf;

    // Lazily-initialised, cached instances
    private EmployeeRepository     repository;
    private EmployeeService        employeeService;
    private ValidationService      validationService;
    private PayrollService         payrollService;
    private SalaryAnalyticsService analyticsService;

    /**
     * @param jdbcUrl  JDBC connection URL
     * @param username database username
     * @param password database password
     */
    public JdbcApplicationFactory(String jdbcUrl, String username, String password) {
        this.dsf = new DataSourceFactory(jdbcUrl, username, password);
        try {
            this.dsf.initSchema();   // create tables if they don't exist
        } catch (SQLException e) {
            throw new RuntimeException("Schema initialisation failed", e);
        }
    }

    /** Exposes the factory so callers can create DAOs or transaction services. */
    public DataSourceFactory getDataSourceFactory() {
        return dsf;
    }

    @Override
    public EmployeeRepository createEmployeeRepository() {
        if (repository == null) {
            repository = new JdbcEmployeeDao(dsf);
        }
        return repository;
    }

    @Override
    public EmployeeService createEmployeeService() {
        if (employeeService == null) {
            employeeService = new EmployeeServiceImpl(createEmployeeRepository());
        }
        return employeeService;
    }

    @Override
    public ValidationService createValidationService() {
        if (validationService == null) {
            validationService = new EmployeeValidationService();
        }
        return validationService;
    }

    @Override
    public PayrollService createPayrollService() {
        if (payrollService == null) {
            payrollService = new PayrollServiceImpl();
        }
        return payrollService;
    }

    @Override
    public EmployeeController createEmployeeController() {
        return new EmployeeController(createEmployeeService(), createValidationService());
    }

    @Override
    public PayrollController createPayrollController() {
        return new PayrollController(createPayrollService());
    }

    @Override
    public SalaryAnalyticsService createSalaryAnalyticsService() {
        if (analyticsService == null) {
            analyticsService = new SalaryAnalyticsServiceImpl(createEmployeeService());
        }
        return analyticsService;
    }
}
