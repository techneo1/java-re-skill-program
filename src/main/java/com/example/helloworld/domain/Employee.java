package com.example.helloworld.domain;

import java.time.LocalDate;
import java.util.Objects;

public abstract sealed class Employee
        permits PermanentEmployee, ContractEmployee {

    private final int id;
    private final String name;
    private final String email;
    private final int departmentId;
    private final String role;
    private double salary;
    private EmployeeStatus status;
    private final LocalDate joiningDate;

    protected Employee(int id, String name, String email, int departmentId,
                       String role, double salary, EmployeeStatus status, LocalDate joiningDate) {
        Objects.requireNonNull(name,        "name must not be null");
        Objects.requireNonNull(email,       "email must not be null");
        Objects.requireNonNull(status,      "status must not be null");
        Objects.requireNonNull(joiningDate, "joiningDate must not be null");
        if (id <= 0)         throw new IllegalArgumentException("id must be positive");
        if (name.isBlank())  throw new IllegalArgumentException("name must not be blank");
        if (email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        if (salary < 0)      throw new IllegalArgumentException("salary must not be negative");

        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.departmentId = departmentId;
        this.role         = role;
        this.salary       = salary;
        this.status       = status;
        this.joiningDate  = joiningDate;
    }

    public abstract String getEmployeeType();

    public int            getId()           { return id; }
    public String         getName()         { return name; }
    public String         getEmail()        { return email; }
    public int            getDepartmentId() { return departmentId; }
    public String         getRole()         { return role; }
    public double         getSalary()       { return salary; }
    public EmployeeStatus getStatus()       { return status; }
    public LocalDate      getJoiningDate()  { return joiningDate; }

    public void setSalary(double salary) {
        if (salary < 0) throw new IllegalArgumentException("salary must not be negative");
        this.salary = salary;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{id=%d, name='%s', email='%s', departmentId=%d, role='%s', " +
                "salary=%.2f, status=%s, joiningDate=%s}",
                getEmployeeType(), id, name, email, departmentId,
                role, salary, status, joiningDate);
    }
}

