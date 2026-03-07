package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {

    void addEmployee(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException;

    void updateEmployee(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException;

    void removeEmployee(int id) throws EmployeeNotFoundException;

    Optional<Employee> getById(int id);

    List<Employee> getAllEmployees();

    List<Employee> getByDepartment(int departmentId);

    List<Employee> getByStatus(EmployeeStatus status);

    List<Employee> getByRole(String role);

    Optional<Employee> getByEmail(String email);

    List<Employee> getBySalaryRange(double min, double max) throws InvalidEmployeeDataException;

    int countEmployees();

    double totalSalary();

    double averageSalary();
}

