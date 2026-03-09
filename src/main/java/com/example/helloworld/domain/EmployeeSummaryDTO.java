package com.example.helloworld.domain;

/**
 * Read-only projection of an {@link Employee} for display and API responses.
 *
 * Using a Record instead of a plain class gives us:
 * - Immutable by design — all fields are final.
 * - Auto-generated {@code equals()}, {@code hashCode()}, {@code toString()}.
 * - Compact, boilerplate-free declaration.
 *
 * The compact constructor derives {@code employeeType} from the sealed
 * hierarchy via a switch expression so callers don't need to know the
 * concrete subtype.
 */
public record EmployeeSummaryDTO(
        int    id,
        String name,
        String role,
        int    departmentId,
        double salary,
        String status,
        String employeeType,
        String extraInfo        // gratuityEligible flag OR contract end date
) {
    /**
     * Compact constructor — validates non-null strings and positive id.
     * All other structural invariants are upheld by the {@code Employee}
     * domain object before this DTO is created.
     */
    public EmployeeSummaryDTO {
        if (id <= 0)               throw new IllegalArgumentException("id must be positive");
        if (name  == null || name.isBlank())  throw new IllegalArgumentException("name must not be blank");
        if (role  == null || role.isBlank())  throw new IllegalArgumentException("role must not be blank");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("status must not be blank");
    }

    /**
     * Factory — builds a summary from any {@link Employee} subtype.
     *
     * Uses a switch expression over the sealed {@code Employee} hierarchy:
     * - {@code PermanentEmployee}  → extraInfo = "gratuityEligible=&lt;bool&gt;"
     * - {@code ContractEmployee}   → extraInfo = "contractEnds=&lt;date&gt;"
     *
     * The switch is exhaustive because {@code Employee} is sealed and
     * only permits {@code PermanentEmployee} and {@code ContractEmployee}.
     */
    public static EmployeeSummaryDTO from(Employee employee) {
        // switch expression over the sealed hierarchy — exhaustive, no default needed
        String extraInfo = switch (employee) {
            case PermanentEmployee pe -> "gratuityEligible=" + pe.isGratuityEligible();
            case ContractEmployee  ce -> "contractEnds="    + ce.getContractEndDate();
        };

        String employeeType = switch (employee) {
            case PermanentEmployee ignored -> "PERMANENT";
            case ContractEmployee  ignored -> "CONTRACT";
        };

        return new EmployeeSummaryDTO(
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                employee.getDepartmentId(),
                employee.getSalary(),
                employee.getStatus().name(),
                employeeType,
                extraInfo
        );
    }
}

