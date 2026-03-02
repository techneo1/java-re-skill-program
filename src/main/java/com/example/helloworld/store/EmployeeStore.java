package com.example.helloworld.store;

import com.example.helloworld.exception.*;
import com.example.helloworld.model.Employee;
import com.example.helloworld.model.EmployeeStatus;

import java.util.List;
import java.util.Optional;

/**
 * Contract for an in-memory Employee data store.
 * Defines CRUD operations and common query methods.
 */
public interface EmployeeStore {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /** Add a new employee. Throws if the id or email already exists. */
    void add(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException;

    /** Replace an existing employee by id. Throws if not found or email is taken. */
    void update(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException;

    /** Remove an employee by id. Throws if not found. */
    void remove(int id) throws EmployeeNotFoundException;

    /** Find an employee by id. Returns empty if not found. */
    Optional<Employee> findById(int id);

    /** Return an unmodifiable snapshot of all employees. */
    List<Employee> findAll();

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Find all employees belonging to a given department. */
    List<Employee> findByDepartment(int departmentId);

    /** Find all employees with the given status. */
    List<Employee> findByStatus(EmployeeStatus status);

    /** Find all employees whose role matches (case-insensitive). */
    List<Employee> findByRole(String role);

    /** Find an employee by email. Returns empty if not found. */
    Optional<Employee> findByEmail(String email);

    /** Find all employees with salary in the range [min, max] inclusive. */
    List<Employee> findBySalaryRange(double min, double max) throws InvalidEmployeeDataException;

    // ── Aggregations ──────────────────────────────────────────────────────────

    /** Total number of employees in the store. */
    int count();

    /** Total salary expenditure across all employees. */
    double totalSalary();

    /** Average salary across all employees. Returns 0.0 if the store is empty. */
    double averageSalary();
}
