package com.example.helloworld.domain;

import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link EmployeeSummaryDTO} record.
 *
 * Covers:
 * - Record accessor correctness
 * - {@link EmployeeSummaryDTO#from(Employee)} factory using switch expression
 *   over the sealed Employee hierarchy (PermanentEmployee → "PERMANENT",
 *   ContractEmployee → "CONTRACT")
 * - Compact constructor validation
 * - Record structural guarantees: equals, hashCode
 */
@DisplayName("EmployeeSummaryDTO")
class EmployeeSummaryDTOTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PermanentEmployee permanent() {
        return new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
    }

    private static ContractEmployee contract() {
        return new ContractEmployee(2, "Bob Smith", "bob@example.com",
                20, "Consultant", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));
    }

    private static PermanentEmployee inactive() {
        return new PermanentEmployee(3, "Carol Rao", "carol@example.com",
                10, "Analyst", 70_000, EmployeeStatus.INACTIVE,
                LocalDate.of(2018, 4, 1), false);
    }

    // ── from() — switch expression over sealed hierarchy ─────────────────────

    @Test
    @DisplayName("from(PermanentEmployee) sets employeeType to 'PERMANENT'")
    void from_permanentEmployee_typeIsPermanent() {
        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(permanent());
        assertEquals("PERMANENT", dto.employeeType());
    }

    @Test
    @DisplayName("from(ContractEmployee) sets employeeType to 'CONTRACT'")
    void from_contractEmployee_typeIsContract() {
        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(contract());
        assertEquals("CONTRACT", dto.employeeType());
    }

    @Test
    @DisplayName("from() copies all common fields from the employee")
    void from_copiesAllFields() {
        PermanentEmployee emp = permanent();
        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(emp);

        assertEquals(emp.getId(),           dto.id());
        assertEquals(emp.getName(),         dto.name());
        assertEquals(emp.getRole(),         dto.role());
        assertEquals(emp.getDepartmentId(), dto.departmentId());
        assertEquals(emp.getSalary(),       dto.salary(), 0.001);
        assertEquals(emp.getStatus(),       dto.status());
    }

    @Test
    @DisplayName("from() preserves INACTIVE status")
    void from_inactiveEmployee_statusIsInactive() {
        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(inactive());
        assertEquals(EmployeeStatus.INACTIVE, dto.status());
    }

    @Test
    @DisplayName("from() for ContractEmployee copies salary and departmentId correctly")
    void from_contractEmployee_copiesFields() {
        ContractEmployee emp = contract();
        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(emp);

        assertEquals(emp.getId(),           dto.id());
        assertEquals(emp.getName(),         dto.name());
        assertEquals(emp.getRole(),         dto.role());
        assertEquals(emp.getDepartmentId(), dto.departmentId());
        assertEquals(emp.getSalary(),       dto.salary(), 0.001);
    }

    // ── Direct construction ───────────────────────────────────────────────────

    @Test
    @DisplayName("directly constructed DTO — all accessors return supplied values")
    void directConstruction_accessorsReturnValues() {
        var dto = new EmployeeSummaryDTO(5, "Dave", "Manager", 30, 95_000,
                EmployeeStatus.ACTIVE, "PERMANENT");
        assertEquals(5,                    dto.id());
        assertEquals("Dave",               dto.name());
        assertEquals("Manager",            dto.role());
        assertEquals(30,                   dto.departmentId());
        assertEquals(95_000,               dto.salary(), 0.001);
        assertEquals(EmployeeStatus.ACTIVE, dto.status());
        assertEquals("PERMANENT",          dto.employeeType());
    }

    // ── Record equals / hashCode ──────────────────────────────────────────────

    @Test
    @DisplayName("two DTOs built from the same employee are equal")
    void equals_sameEmployee_equal() {
        EmployeeSummaryDTO a = EmployeeSummaryDTO.from(permanent());
        EmployeeSummaryDTO b = EmployeeSummaryDTO.from(permanent());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("DTOs built from different employees are not equal")
    void equals_differentEmployees_notEqual() {
        EmployeeSummaryDTO a = EmployeeSummaryDTO.from(permanent());
        EmployeeSummaryDTO b = EmployeeSummaryDTO.from(contract());
        assertNotEquals(a, b);
    }

    // ── Compact constructor validation ────────────────────────────────────────

    @Test
    @DisplayName("id <= 0 throws IllegalArgumentException")
    void validation_idZero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(0, "X", "Eng", 1, 1000,
                        EmployeeStatus.ACTIVE, "PERMANENT"));
    }

    @Test
    @DisplayName("blank name throws IllegalArgumentException")
    void validation_blankName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(1, "  ", "Eng", 1, 1000,
                        EmployeeStatus.ACTIVE, "PERMANENT"));
    }

    @Test
    @DisplayName("blank role throws IllegalArgumentException")
    void validation_blankRole_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(1, "Alice", "", 1, 1000,
                        EmployeeStatus.ACTIVE, "PERMANENT"));
    }

    @Test
    @DisplayName("negative salary throws IllegalArgumentException")
    void validation_negativeSalary_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(1, "Alice", "Eng", 1, -1,
                        EmployeeStatus.ACTIVE, "PERMANENT"));
    }

    @Test
    @DisplayName("null status throws IllegalArgumentException")
    void validation_nullStatus_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(1, "Alice", "Eng", 1, 1000,
                        null, "PERMANENT"));
    }

    @Test
    @DisplayName("blank employeeType throws IllegalArgumentException")
    void validation_blankEmployeeType_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeeSummaryDTO(1, "Alice", "Eng", 1, 1000,
                        EmployeeStatus.ACTIVE, "  "));
    }
}

