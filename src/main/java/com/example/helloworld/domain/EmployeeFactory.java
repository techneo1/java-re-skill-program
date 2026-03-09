package com.example.helloworld.domain;

import java.time.LocalDate;

/**
 * Factory Method pattern — centralises employee creation.
 *
 * Instead of callers knowing which concrete subclass to instantiate,
 * they call a named factory method that makes the intent explicit and
 * delegates to the appropriate Builder internally.
 *
 * Benefits:
 *  - Single place to change construction logic for each employee type.
 *  - Callers are decoupled from concrete classes (depend only on Employee).
 *  - New employee types can be added here without touching existing call-sites.
 */
public final class EmployeeFactory {

    private EmployeeFactory() { /* utility class — no instances */ }

    /**
     * Creates a {@link PermanentEmployee} using the Builder pattern internally.
     *
     * @param id               unique employee id (positive)
     * @param name             full name (non-blank)
     * @param email            corporate email (must contain '@')
     * @param departmentId     department the employee belongs to (positive)
     * @param role             job role / title (non-blank)
     * @param salary           base annual salary (≥ 0)
     * @param joiningDate      date the employee joined the organisation
     * @param gratuityEligible whether the employee qualifies for gratuity
     * @return a new, fully-validated {@link PermanentEmployee}
     */
    public static PermanentEmployee createPermanentEmployee(
            int id, String name, String email, int departmentId,
            String role, double salary, LocalDate joiningDate,
            boolean gratuityEligible) {

        return PermanentEmployee.builder()
                .id(id)
                .name(name)
                .email(email)
                .departmentId(departmentId)
                .role(role)
                .salary(salary)
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(joiningDate)
                .gratuityEligible(gratuityEligible)
                .build();
    }

    /**
     * Creates a {@link ContractEmployee} using the Builder pattern internally.
     *
     * @param id              unique employee id (positive)
     * @param name            full name (non-blank)
     * @param email           corporate email (must contain '@')
     * @param departmentId    department the employee belongs to (positive)
     * @param role            job role / title (non-blank)
     * @param salary          base annual salary (≥ 0)
     * @param joiningDate     date the employee joined the organisation
     * @param contractEndDate date the contract expires (must be after joiningDate)
     * @return a new, fully-validated {@link ContractEmployee}
     */
    public static ContractEmployee createContractEmployee(
            int id, String name, String email, int departmentId,
            String role, double salary, LocalDate joiningDate,
            LocalDate contractEndDate) {

        return ContractEmployee.builder()
                .id(id)
                .name(name)
                .email(email)
                .departmentId(departmentId)
                .role(role)
                .salary(salary)
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(joiningDate)
                .contractEndDate(contractEndDate)
                .build();
    }
}

