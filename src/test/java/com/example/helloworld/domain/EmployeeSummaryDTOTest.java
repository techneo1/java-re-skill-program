package com.example.helloworld.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmployeeSummaryDTO")
class EmployeeSummaryDTOTest {

    private static final LocalDate JOINING   = LocalDate.of(2020, 1, 1);
    private static final LocalDate END_DATE  = LocalDate.of(2030, 1, 1);

    // ── Record structural guarantees ──────────────────────────────────────────

    @Test
    @DisplayName("from(PermanentEmployee) — employeeType is PERMANENT")
    void from_permanent_employeeTypeLabel() {
        PermanentEmployee pe = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(pe);

        assertEquals("PERMANENT", dto.employeeType());
    }

    @Test
    @DisplayName("from(ContractEmployee) — employeeType is CONTRACT")
    void from_contract_employeeTypeLabel() {
        ContractEmployee ce = new ContractEmployee(
                2, "Bob", "bob@x.com", 10, "Designer", 60_000,
                EmployeeStatus.ACTIVE, JOINING, END_DATE);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(ce);

        assertEquals("CONTRACT", dto.employeeType());
    }

    @Test
    @DisplayName("from(PermanentEmployee) — extraInfo contains gratuityEligible flag")
    void from_permanent_extraInfoContainsGratuity() {
        PermanentEmployee pe = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(pe);

        assertTrue(dto.extraInfo().contains("gratuityEligible"),
                "extraInfo must mention gratuityEligible");
        assertTrue(dto.extraInfo().contains("true"),
                "extraInfo must reflect the actual flag value");
    }

    @Test
    @DisplayName("from(PermanentEmployee) — extraInfo reflects gratuityEligible=false")
    void from_permanent_extraInfoGratuityFalse() {
        PermanentEmployee pe = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, false);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(pe);

        assertTrue(dto.extraInfo().contains("false"));
    }

    @Test
    @DisplayName("from(ContractEmployee) — extraInfo contains contract end date")
    void from_contract_extraInfoContainsEndDate() {
        ContractEmployee ce = new ContractEmployee(
                2, "Bob", "bob@x.com", 10, "Designer", 60_000,
                EmployeeStatus.ACTIVE, JOINING, END_DATE);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(ce);

        assertTrue(dto.extraInfo().contains("contractEnds"),
                "extraInfo must mention contractEnds");
        assertTrue(dto.extraInfo().contains(END_DATE.toString()),
                "extraInfo must contain the actual end date");
    }

    @Test
    @DisplayName("from — all common fields are projected correctly")
    void from_commonFieldsMatchEmployee() {
        PermanentEmployee pe = new PermanentEmployee(
                7, "Carol", "carol@x.com", 20, "Manager", 110_000,
                EmployeeStatus.ACTIVE, JOINING, true);

        EmployeeSummaryDTO dto = EmployeeSummaryDTO.from(pe);

        assertAll(
                () -> assertEquals(7,           dto.id()),
                () -> assertEquals("Carol",     dto.name()),
                () -> assertEquals("Manager",   dto.role()),
                () -> assertEquals(20,          dto.departmentId()),
                () -> assertEquals(110_000,     dto.salary(), 0.001),
                () -> assertEquals("ACTIVE",    dto.status())
        );
    }

    // ── Record immutability & equality ────────────────────────────────────────

    @Test
    @DisplayName("two DTOs built from identical employees are equal (Record equals)")
    void recordEquals_identicalEmployees() {
        PermanentEmployee pe1 = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);
        PermanentEmployee pe2 = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);

        assertEquals(EmployeeSummaryDTO.from(pe1), EmployeeSummaryDTO.from(pe2),
                "Records with identical components must be equal");
    }

    @Test
    @DisplayName("two DTOs built from different employees are not equal")
    void recordEquals_differentEmployees() {
        PermanentEmployee pe1 = new PermanentEmployee(
                1, "Alice", "alice@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);
        PermanentEmployee pe2 = new PermanentEmployee(
                2, "Bob", "bob@x.com", 10, "Engineer", 80_000,
                EmployeeStatus.ACTIVE, JOINING, true);

        assertNotEquals(EmployeeSummaryDTO.from(pe1), EmployeeSummaryDTO.from(pe2));
    }

    // ── Compact constructor validation ────────────────────────────────────────

    @Test
    @DisplayName("constructor — throws for id <= 0")
    void constructor_invalidId_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmployeeSummaryDTO(0, "Alice", "Engineer", 10,
                        80_000, "ACTIVE", "PERMANENT", "gratuityEligible=true"));
    }

    @ParameterizedTest(name = "blank name: \"{0}\"")
    @DisplayName("constructor — throws for blank name")
    @CsvSource({"''", "' '"})
    void constructor_blankName_throws(String name) {
        assertThrows(IllegalArgumentException.class, () ->
                new EmployeeSummaryDTO(1, name, "Engineer", 10,
                        80_000, "ACTIVE", "PERMANENT", "gratuityEligible=true"));
    }

    @ParameterizedTest(name = "blank role: \"{0}\"")
    @DisplayName("constructor — throws for blank role")
    @CsvSource({"''", "' '"})
    void constructor_blankRole_throws(String role) {
        assertThrows(IllegalArgumentException.class, () ->
                new EmployeeSummaryDTO(1, "Alice", role, 10,
                        80_000, "ACTIVE", "PERMANENT", "gratuityEligible=true"));
    }
}

