package com.example.helloworld.factory;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.controller.SalaryAnalyticsController;
import com.example.helloworld.repository.EmployeeRepository;
import com.example.helloworld.repository.inmemory.InMemoryEmployeeRepository;
import com.example.helloworld.service.*;

import java.util.Set;

/**
 * Concrete Abstract Factory that wires the entire application stack
 * using in-memory storage.
 *
 * The ValidationService is wired with:
 *   - the shared repository (for unique-email checks), and
 *   - the default set of valid department IDs (10, 20, 30).
 *
 * Swap the validDepartmentIds set or supply a different repository to
 * customise validation without touching any other layer.
 */
public class InMemoryApplicationFactory implements ApplicationFactory {

    /**
     * Default known department IDs used for referential-integrity validation.
     * In a real system this would be loaded from a DepartmentRepository.
     */
    private static final Set<Integer> DEFAULT_DEPARTMENT_IDS = Set.of(10, 20, 30);

    // Lazily-initialised, cached instances
    private EmployeeRepository     repository;
    private EmployeeService        employeeService;
    private ValidationService      validationService;
    private PayrollService         payrollService;
    private SalaryAnalyticsService salaryAnalyticsService;

    @Override
    public EmployeeRepository createEmployeeRepository() {
        if (repository == null) {
            repository = new InMemoryEmployeeRepository();
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

    /**
     * Creates an {@link EmployeeValidationService} wired with:
     * <ul>
     *   <li>the shared repository — enables unique-email checking</li>
     *   <li>{@link #DEFAULT_DEPARTMENT_IDS} — enables department-existence checking</li>
     * </ul>
     */
    @Override
    public ValidationService createValidationService() {
        if (validationService == null) {
            validationService = new EmployeeValidationService(
                    createEmployeeRepository(),
                    DEFAULT_DEPARTMENT_IDS
            );
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
    public SalaryAnalyticsService createSalaryAnalyticsService() {
        if (salaryAnalyticsService == null) {
            salaryAnalyticsService = new SalaryAnalyticsServiceImpl();
        }
        return salaryAnalyticsService;
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
    public SalaryAnalyticsController createSalaryAnalyticsController() {
        return new SalaryAnalyticsController(createEmployeeService(), createSalaryAnalyticsService());
    }
}
