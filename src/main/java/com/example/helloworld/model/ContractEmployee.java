package com.example.helloworld.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a contract-based employee with a fixed contract end date.
 * Permitted subclass of the sealed Employee hierarchy.
 *
 * Additional fields:
 *  - contractEndDate : the date on which the contract expires
 */
public final class ContractEmployee extends Employee {

    private LocalDate contractEndDate;

    // ── Constructor ──────────────────────────────────────────────────────────
    public ContractEmployee(int id,
                            String name,
                            String email,
                            int departmentId,
                            String role,
                            double salary,
                            EmployeeStatus status,
                            LocalDate joiningDate,
                            LocalDate contractEndDate) {
        super(id, name, email, departmentId, role, salary, status, joiningDate);
        Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        if (!contractEndDate.isAfter(joiningDate))
            throw new IllegalArgumentException("contractEndDate must be after joiningDate");
        this.contractEndDate = contractEndDate;
    }

    // ── Abstract method implementation ───────────────────────────────────────
    @Override
    public String getEmployeeType() {
        return "ContractEmployee";
    }

    // ── Getter / Setter ──────────────────────────────────────────────────────
    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        this.contractEndDate = contractEndDate;
    }

    /** Returns true if the contract has already expired relative to today. */
    public boolean isExpired() {
        return LocalDate.now().isAfter(contractEndDate);
    }

    // ── equals / hashCode ────────────────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractEmployee other)) return false;
        return super.equals(other)
                && Objects.equals(contractEndDate, other.contractEndDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contractEndDate);
    }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return super.toString() +
               String.format(", contractEndDate=%s}", contractEndDate)
                       .replace("}}", "}");
    }
}

