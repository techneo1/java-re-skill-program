package com.example.helloworld.service;

import com.example.helloworld.domain.ContractEmployee;
import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.EmployeeRepository;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Validates Employee fields against business rules.
 *
 * <p>Three categories of validation:
 * <ol>
 *   <li><b>Field rules</b> — id positive, name/email not blank, email contains '@',
 *       salary non-negative, status not null/inactive, departmentId positive,
 *       contract not expired.</li>
 *   <li><b>Uniqueness</b> — email must not already belong to a <em>different</em>
 *       employee in the repository (throws {@link DuplicateEmailException}).</li>
 *   <li><b>Referential integrity</b> — departmentId must be present in the
 *       known-departments set (throws {@link DepartmentNotFoundException}).</li>
 * </ol>
 *
 * <p>The repository and departments set are optional (constructor overloads).
 * When omitted the corresponding checks are silently skipped, keeping the
 * service usable in unit tests that don't need a full stack.
 */
public class EmployeeValidationService implements ValidationService {

    /** Repository used for duplicate-email look-up (may be null → check skipped). */
    private final EmployeeRepository repository;

    /**
     * Set of valid department IDs.
     * {@code null} or empty → department-existence check is skipped.
     */
    private final Set<Integer> validDepartmentIds;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor — enables all three validation categories.
     *
     * @param repository        used for unique-email look-up; {@code null} → skip
     * @param validDepartmentIds set of known department IDs; {@code null} or empty → skip
     */
    public EmployeeValidationService(EmployeeRepository repository,
                                     Set<Integer> validDepartmentIds) {
        this.repository         = repository;
        this.validDepartmentIds = validDepartmentIds != null
                ? Collections.unmodifiableSet(validDepartmentIds)
                : Collections.emptySet();
    }

    /**
     * Field-rules-only constructor (no repository, no department registry).
     * Backward-compatible with existing usages and tests.
     */
    public EmployeeValidationService() {
        this(null, null);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates all applicable rules for the given employee.
     *
     * <p>Rules checked in order:
     * <ol>
     *   <li>id positive</li>
     *   <li>name not blank</li>
     *   <li>email not blank and contains '@'</li>
     *   <li><b>salary not negative</b> — throws {@link ValidationException}</li>
     *   <li>status not null and not INACTIVE</li>
     *   <li>departmentId positive</li>
     *   <li>ContractEmployee contract not expired</li>
     *   <li><b>email uniqueness</b> — throws {@link DuplicateEmailException}
     *       when a different employee already owns the email</li>
     *   <li><b>department exists</b> — throws {@link DepartmentNotFoundException}
     *       when departmentId is not in the known-departments set</li>
     * </ol>
     *
     * @param employee the employee to validate (must not be {@code null})
     * @throws ValidationException          on field-rule violations
     * @throws DuplicateEmailException      when the email is already taken
     * @throws DepartmentNotFoundException  when the department does not exist
     */
    @Override
    public void validate(Employee employee)
            throws ValidationException, DuplicateEmailException, DepartmentNotFoundException {

        Objects.requireNonNull(employee, "employee must not be null");

        // ── 1. id ─────────────────────────────────────────────────────────────
        if (employee.getId() <= 0)
            throw new ValidationException("id", employee.getId(), "must be positive");

        // ── 2. name ───────────────────────────────────────────────────────────
        if (employee.getName().isBlank())
            throw new ValidationException("name", employee.getName(), "must not be blank");

        // ── 3. email format ───────────────────────────────────────────────────
        if (employee.getEmail().isBlank())
            throw new ValidationException("email", employee.getEmail(), "must not be blank");

        if (!employee.getEmail().contains("@"))
            throw new ValidationException("email", employee.getEmail(), "must contain '@'");

        // ── 4. salary cannot be negative ──────────────────────────────────────
        if (employee.getSalary() < 0)
            throw new ValidationException("salary", employee.getSalary(), "must not be negative");

        // ── 5. status ─────────────────────────────────────────────────────────
        if (employee.getStatus() == null)
            throw new ValidationException("status", null, "must not be null");

        if (employee.getStatus() == EmployeeStatus.INACTIVE)
            throw new ValidationException("status", employee.getStatus(),
                    "cannot add or update an INACTIVE employee");

        // ── 6. departmentId positive ──────────────────────────────────────────
        if (employee.getDepartmentId() <= 0)
            throw new ValidationException("departmentId", employee.getDepartmentId(), "must be positive");

        // ── 7. Contract expiry ────────────────────────────────────────────────
        if (employee instanceof ContractEmployee ce && ce.isExpired())
            throw new ValidationException("contractEndDate", ce.getContractEndDate(),
                    "contract has already expired");

        // ── 8. Unique email — email must not belong to a different employee ───
        validateUniqueEmail(employee);

        // ── 9. Department must exist ──────────────────────────────────────────
        if (!validDepartmentIds.isEmpty()
                && !validDepartmentIds.contains(employee.getDepartmentId())) {
            throw new DepartmentNotFoundException(employee.getDepartmentId());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Checks email uniqueness against the repository without the
     * lambda-rethrow anti-pattern.
     *
     * @throws DuplicateEmailException when the email belongs to a different employee
     */
    private void validateUniqueEmail(Employee employee) throws DuplicateEmailException {
        if (repository == null) return;

        var conflict = repository.findByEmail(employee.getEmail());
        if (conflict.isPresent() && conflict.get().getId() != employee.getId()) {
            throw new DuplicateEmailException(employee.getEmail());
        }
    }
}
