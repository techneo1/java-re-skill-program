package com.example.helloworld.factory;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.controller.SalaryAnalyticsController;
import com.example.helloworld.repository.EmployeeRepository;
import com.example.helloworld.repository.inmemory.InMemoryEmployeeRepository;
import com.example.helloworld.service.*;

/**
 * Concrete Abstract Factory that wires the entire application stack
 * using in-memory storage.
 *
 * Each component is created lazily and cached so every call to
 * createEmployeeController() / createPayrollController() returns an
 * object backed by the same shared repository and services.
 */
public class InMemoryApplicationFactory implements ApplicationFactory {

    // Lazily-initialised, cached instances
    private EmployeeRepository       repository;
    private EmployeeService          employeeService;
    private ValidationService        validationService;
    private PayrollService           payrollService;
    private SalaryAnalyticsService   salaryAnalyticsService;

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
            // PayrollServiceImpl() uses the singleton PayrollStrategyRegistry internally
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
