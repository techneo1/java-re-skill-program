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

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int          id;
        private String       name;
        private String       email;
        private int          departmentId;
        private String       role;
        private double       salary;
        private EmployeeStatus status   = EmployeeStatus.ACTIVE;
        private LocalDate    joiningDate;
        private boolean      gratuityEligible;

        private Builder() {}

        public Builder id(int id)                          { this.id = id;                       return this; }
        public Builder name(String name)                   { this.name = name;                   return this; }
        public Builder email(String email)                 { this.email = email;                 return this; }
        public Builder departmentId(int departmentId)      { this.departmentId = departmentId;   return this; }
        public Builder role(String role)                   { this.role = role;                   return this; }
        public Builder salary(double salary)               { this.salary = salary;               return this; }
        public Builder status(EmployeeStatus status)       { this.status = status;               return this; }
        public Builder joiningDate(LocalDate joiningDate)  { this.joiningDate = joiningDate;     return this; }
        public Builder gratuityEligible(boolean eligible)  { this.gratuityEligible = eligible;   return this; }

        public PermanentEmployee build() {
            Objects.requireNonNull(name,        "name must not be null");
            Objects.requireNonNull(email,       "email must not be null");
            Objects.requireNonNull(joiningDate, "joiningDate must not be null");
            return new PermanentEmployee(id, name, email, departmentId,
                    role, salary, status, joiningDate, gratuityEligible);
        }
    }

    // ── existing code ─────────────────────────────────────────────────────────

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
