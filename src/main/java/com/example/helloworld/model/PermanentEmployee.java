package com.example.helloworld.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a full-time permanent employee.
 * Permitted subclass of the sealed Employee hierarchy.
 *
 * Additional fields:
 *  - gratuityEligible : whether the employee qualifies for gratuity benefits
 */
public final class PermanentEmployee extends Employee {

    private final boolean gratuityEligible;

    // ── Constructor ──────────────────────────────────────────────────────────
    public PermanentEmployee(int id,
                             String name,
                             String email,
                             int departmentId,
                             String role,
                             double salary,
                             EmployeeStatus status,
                             LocalDate joiningDate,
                             boolean gratuityEligible) {
        super(id, name, email, departmentId, role, salary, status, joiningDate);
        this.gratuityEligible = gratuityEligible;
    }

    // ── Abstract method implementation ───────────────────────────────────────
    @Override
    public String getEmployeeType() {
        return "PermanentEmployee";
    }

    // ── Getter ───────────────────────────────────────────────────────────────
    public boolean isGratuityEligible() {
        return gratuityEligible;
    }

    // ── equals / hashCode (delegate to parent — identity is id) ─────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermanentEmployee other)) return false;
        return super.equals(other)
                && gratuityEligible == other.gratuityEligible;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gratuityEligible);
    }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return super.toString() +
               String.format(", gratuityEligible=%b}", gratuityEligible)
                       .replace("}}", "}");   // clean up trailing brace from super
    }
}

