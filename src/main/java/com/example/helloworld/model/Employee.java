package com.example.helloworld.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Sealed abstract base class for all Employee types.
 * Only PermanentEmployee and ContractEmployee are permitted subclasses.
 *
 * OOP concepts applied:
 *  - Encapsulation  : all fields private, accessed via getters
 *  - Abstraction    : abstract getEmployeeType() forces subclasses to identify themselves
 *  - Inheritance    : PermanentEmployee / ContractEmployee extend this class
 *  - Sealed class   : restricts the class hierarchy to known, controlled subtypes
 */
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

    // ── Constructor ──────────────────────────────────────────────────────────
    protected Employee(int id,
                       String name,
                       String email,
                       int departmentId,
                       String role,
                       double salary,
                       EmployeeStatus status,
                       LocalDate joiningDate) {

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

    // ── Abstract behaviour ───────────────────────────────────────────────────
    /** Returns a human-readable label for the concrete employee type. */
    public abstract String getEmployeeType();

    // ── Getters ──────────────────────────────────────────────────────────────
    public int           getId()           { return id; }
    public String        getName()         { return name; }
    public String        getEmail()        { return email; }
    public int           getDepartmentId() { return departmentId; }
    public String        getRole()         { return role; }
    public double        getSalary()       { return salary; }
    public EmployeeStatus getStatus()      { return status; }
    public LocalDate     getJoiningDate()  { return joiningDate; }

    // ── Setters for mutable fields ───────────────────────────────────────────
    public void setSalary(double salary) {
        if (salary < 0) throw new IllegalArgumentException("salary must not be negative");
        this.salary = salary;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    // ── equals / hashCode (identity based on id) ────────────────────────────
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

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "%s{id=%d, name='%s', email='%s', departmentId=%d, role='%s', " +
                "salary=%.2f, status=%s, joiningDate=%s}",
                getEmployeeType(), id, name, email, departmentId,
                role, salary, status, joiningDate);
    }
}

