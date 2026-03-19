package com.example.helloworld.domain;

/**
 * Immutable lightweight view of a single employee's key salary details.
 *
 * <p>Implemented as a <b>Record</b> — the compiler auto-generates the
 * canonical constructor, accessors, {@code equals()}, {@code hashCode()},
 * and {@code toString()}.
 *
 * <p>Used as the element type in:
 * <ul>
 *   <li>{@link com.example.helloworld.service.SalaryAnalyticsService#top5BySalary()}</li>
 *   <li>{@link com.example.helloworld.service.SalaryAnalyticsService#partitionByStatus()}</li>
 * </ul>
 *
 * @param id           employee ID
 * @param name         full name
 * @param role         job role / title
 * @param departmentId department the employee belongs to
 * @param salary       current salary
 * @param status       {@link EmployeeStatus#ACTIVE} or {@link EmployeeStatus#INACTIVE}
 * @param employeeType human-readable type tag — {@code "PERMANENT"} or {@code "CONTRACT"}
 */
public record EmployeeSummaryDTO(
        int            id,
        String         name,
        String         role,
        int            departmentId,
        double         salary,
        EmployeeStatus status,
        String         employeeType
) {
    /** Compact canonical constructor — validates required fields. */
    public EmployeeSummaryDTO {
        if (id <= 0)                        throw new IllegalArgumentException("id must be positive");
        if (name         == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        if (role         == null || role.isBlank())
            throw new IllegalArgumentException("role must not be blank");
        if (salary < 0)                     throw new IllegalArgumentException("salary must not be negative");
        if (status       == null)           throw new IllegalArgumentException("status must not be null");
        if (employeeType == null || employeeType.isBlank())
            throw new IllegalArgumentException("employeeType must not be blank");
    }

    /**
     * Factory — builds an {@link EmployeeSummaryDTO} from any {@link Employee}.
     *
     * <p>Uses a <b>switch expression</b> on the <b>sealed class hierarchy</b>
     * to derive the {@code employeeType} tag without an explicit {@code instanceof}
     * chain.  The compiler enforces exhaustiveness — a missing {@code permits}
     * subtype is a compile error, not a silent runtime gap.
     *
     * <pre>{@code
     * String type = switch (employee) {
     *     case PermanentEmployee pe -> "PERMANENT";
     *     case ContractEmployee  ce -> "CONTRACT";
     * };
     * }</pre>
     */
    public static EmployeeSummaryDTO from(Employee employee) {
        // Switch expression over the sealed hierarchy — exhaustive, no default needed
        String employeeType = switch (employee) {
            case PermanentEmployee pe -> "PERMANENT";
            case ContractEmployee  ce -> "CONTRACT";
        };

        return new EmployeeSummaryDTO(
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                employee.getDepartmentId(),
                employee.getSalary(),
                employee.getStatus(),
                employeeType
        );
    }
}

