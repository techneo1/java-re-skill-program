package com.example.helloworld.factory;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.repository.EmployeeRepository;
import com.example.helloworld.repository.inmemory.InMemoryEmployeeRepository;
import com.example.helloworld.service.*;

/**
 * Abstract Factory pattern — defines the interface for creating a family of
 * related objects: repository, services, and controllers.
 *
 * Concrete implementations swap the entire stack (e.g. in-memory vs database)
 * without any change to application code.
 *
 * Current implementations:
 *   {@link InMemoryApplicationFactory} — fully in-memory wiring (production default)
 *
 * Usage:
 * <pre>{@code
 *   ApplicationFactory factory = new InMemoryApplicationFactory();
 *   EmployeeController  empCtrl = factory.createEmployeeController();
 *   PayrollController   payCtrl = factory.createPayrollController();
 * }</pre>
 */
public interface ApplicationFactory {

    /** Creates (or returns a cached) {@link EmployeeRepository} for this application context. */
    EmployeeRepository createEmployeeRepository();

    /** Creates an {@link EmployeeService} wired to this factory's repository. */
    EmployeeService createEmployeeService();

    /** Creates a {@link ValidationService} for employee validation. */
    ValidationService createValidationService();

    /** Creates a {@link PayrollService} wired to the singleton strategy registry. */
    PayrollService createPayrollService();

    /** Creates a fully-wired {@link EmployeeController}. */
    EmployeeController createEmployeeController();

    /** Creates a fully-wired {@link PayrollController}. */
    PayrollController createPayrollController();
}

