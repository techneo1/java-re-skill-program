package com.example.helloworld.repository;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {

    void add(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException;

    void update(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException;

    void remove(int id) throws EmployeeNotFoundException;

    Optional<Employee> findById(int id);

    List<Employee> findAll();

    List<Employee> findByDepartment(int departmentId);

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByRole(String role);

    Optional<Employee> findByEmail(String email);

    List<Employee> findBySalaryRange(double min, double max) throws InvalidEmployeeDataException;

    int count();

    double totalSalary();

    double averageSalary();
}

