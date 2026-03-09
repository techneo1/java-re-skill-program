package com.example.helloworld.controller;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;
import com.example.helloworld.service.EmployeeService;
import com.example.helloworld.service.ValidationService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller layer for employee operations.
 * Accepts requests, delegates to ValidationService + EmployeeService,
 * and handles all checked exceptions — no exception leaks to the caller.
 *
 * Layer: Controller → Service → Repository
 */
public class EmployeeController {

    private static final String SOURCE = "EmployeeController";

    private final EmployeeService    employeeService;
    private final ValidationService  validationService;

    public EmployeeController(EmployeeService employeeService,
                              ValidationService validationService) {
        this.employeeService   = Objects.requireNonNull(employeeService,   "employeeService must not be null");
        this.validationService = Objects.requireNonNull(validationService, "validationService must not be null");
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    /**
     * Validate and add a new employee.
     * Prints a success or error message; never throws.
     */
    public void addEmployee(Employee employee) {
        try {
            validationService.validate(employee);
            employeeService.addEmployee(employee);
            System.out.printf("[EmployeeController] Added   : id=%-3d  name=%s%n",
                    employee.getId(), employee.getName());
        } catch (EmployeeException e) {
            GlobalExceptionHandler.handleAndLog(e, SOURCE);
        }
    }

    /**
     * Validate and update an existing employee.
     * Prints a success or error message; never throws.
     */
    public void updateEmployee(Employee employee) {
        try {
            validationService.validate(employee);
            employeeService.updateEmployee(employee);
            System.out.printf("[EmployeeController] Updated : id=%-3d  name=%s%n",
                    employee.getId(), employee.getName());
        } catch (EmployeeException e) {
            GlobalExceptionHandler.handleAndLog(e, SOURCE);
        }
    }

    /**
     * Remove an employee by id.
     * Prints a success or error message; never throws.
     */
    public void removeEmployee(int id) {
        try {
            employeeService.removeEmployee(id);
            System.out.printf("[EmployeeController] Removed : id=%d%n", id);
        } catch (EmployeeException e) {
            GlobalExceptionHandler.handleAndLog(e, SOURCE);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Optional<Employee> getById(int id)          { return employeeService.getById(id); }
    public Optional<Employee> getByEmail(String email) { return employeeService.getByEmail(email); }
    public List<Employee>     getAllEmployees()         { return employeeService.getAllEmployees(); }
    public List<Employee>     getByDepartment(int deptId) { return employeeService.getByDepartment(deptId); }
    public List<Employee>     getByStatus(EmployeeStatus status) { return employeeService.getByStatus(status); }
    public List<Employee>     getByRole(String role)   { return employeeService.getByRole(role); }

    /**
     * Query employees by salary range.
     * Returns an empty list and prints an error if the range is invalid; never throws.
     */
    public List<Employee> getBySalaryRange(double min, double max) {
        try {
            return employeeService.getBySalaryRange(min, max);
        } catch (InvalidEmployeeDataException e) {
            GlobalExceptionHandler.handleAndLog(e, SOURCE);
            return List.of();
        }
    }

    // ── Aggregations ──────────────────────────────────────────────────────────

    public int    countEmployees() { return employeeService.countEmployees(); }
    public double totalSalary()    { return employeeService.totalSalary();    }
    public double averageSalary()  { return employeeService.averageSalary();  }
}
