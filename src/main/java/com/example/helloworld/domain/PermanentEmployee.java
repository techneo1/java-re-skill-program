package com.example.helloworld.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class PermanentEmployee extends Employee {

    private final boolean gratuityEligible;

    public PermanentEmployee(int id, String name, String email, int departmentId,
                             String role, double salary, EmployeeStatus status,
                             LocalDate joiningDate, boolean gratuityEligible) {
        super(id, name, email, departmentId, role, salary, status, joiningDate);
        this.gratuityEligible = gratuityEligible;
    }

    @Override
    public String getEmployeeType() { return "PermanentEmployee"; }

    public boolean isGratuityEligible() { return gratuityEligible; }

    @Override
    protected String extraFields() {
        return String.format(", gratuityEligible=%b", gratuityEligible);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermanentEmployee other)) return false;
        return super.equals(other) && gratuityEligible == other.gratuityEligible;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gratuityEligible);
    }
}
