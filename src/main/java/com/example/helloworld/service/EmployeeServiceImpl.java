package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.EmployeeRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeServiceImpl(EmployeeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void addEmployee(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException {
        repository.add(employee);
    }

    @Override
    public void updateEmployee(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException {
        repository.update(employee);
    }

    @Override
    public void removeEmployee(int id) throws EmployeeNotFoundException {
        repository.remove(id);
    }

    @Override
    public Optional<Employee> getById(int id) {
        return repository.findById(id);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    @Override
    public List<Employee> getByDepartment(int departmentId) {
        return repository.findByDepartment(departmentId);
    }

    @Override
    public List<Employee> getByStatus(EmployeeStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Employee> getByRole(String role) {
        return repository.findByRole(role);
    }

    @Override
    public Optional<Employee> getByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public List<Employee> getBySalaryRange(double min, double max) throws InvalidEmployeeDataException {
        return repository.findBySalaryRange(min, max);
    }

    @Override
    public int countEmployees() {
        return repository.count();
    }

    @Override
    public double totalSalary() {
        return repository.totalSalary();
    }

    @Override
    public double averageSalary() {
        return repository.averageSalary();
    }
}

