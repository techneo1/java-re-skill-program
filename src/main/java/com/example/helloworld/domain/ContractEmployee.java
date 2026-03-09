package com.example.helloworld.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class ContractEmployee extends Employee {

    private LocalDate contractEndDate;

    public ContractEmployee(int id, String name, String email, int departmentId,
                            String role, double salary, EmployeeStatus status,
                            LocalDate joiningDate, LocalDate contractEndDate) {
        super(id, name, email, departmentId, role, salary, status, joiningDate);
        Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        if (!contractEndDate.isAfter(joiningDate))
            throw new IllegalArgumentException("contractEndDate must be after joiningDate");
        this.contractEndDate = contractEndDate;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int            id;
        private String         name;
        private String         email;
        private int            departmentId;
        private String         role;
        private double         salary;
        private EmployeeStatus status      = EmployeeStatus.ACTIVE;
        private LocalDate      joiningDate;
        private LocalDate      contractEndDate;

        private Builder() {}

        public Builder id(int id)                               { this.id = id;                         return this; }
        public Builder name(String name)                        { this.name = name;                     return this; }
        public Builder email(String email)                      { this.email = email;                   return this; }
        public Builder departmentId(int departmentId)           { this.departmentId = departmentId;     return this; }
        public Builder role(String role)                        { this.role = role;                     return this; }
        public Builder salary(double salary)                    { this.salary = salary;                 return this; }
        public Builder status(EmployeeStatus status)            { this.status = status;                 return this; }
        public Builder joiningDate(LocalDate joiningDate)       { this.joiningDate = joiningDate;       return this; }
        public Builder contractEndDate(LocalDate endDate)       { this.contractEndDate = endDate;       return this; }

        public ContractEmployee build() {
            Objects.requireNonNull(name,            "name must not be null");
            Objects.requireNonNull(email,           "email must not be null");
            Objects.requireNonNull(joiningDate,     "joiningDate must not be null");
            Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
            return new ContractEmployee(id, name, email, departmentId,
                    role, salary, status, joiningDate, contractEndDate);
        }
    }

    // ── existing code ─────────────────────────────────────────────────────────

    @Override
    public String getEmployeeType() { return "ContractEmployee"; }

    public LocalDate getContractEndDate() { return contractEndDate; }

    public void setContractEndDate(LocalDate contractEndDate) {
        Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        this.contractEndDate = contractEndDate;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(contractEndDate);
    }

    @Override
    protected String extraFields() {
        return String.format(", contractEndDate=%s", contractEndDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractEmployee other)) return false;
        return super.equals(other) && Objects.equals(contractEndDate, other.contractEndDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contractEndDate);
    }
}
