package com.example.helloworld.domain;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link DepartmentSalaryReport} record.
 *
 * Covers:
 * - Correct field storage (record accessor methods)
 * - Compact constructor validation (all illegal argument cases)
 * - Record structural guarantees: equals, hashCode, toString
 */
@DisplayName("DepartmentSalaryReport")
class DepartmentSalaryReportTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("all accessors return the values passed to the constructor")
    void accessors_returnConstructedValues() {
        var r = new DepartmentSalaryReport(10, 3, 270_000, 90_000, 80_000, 100_000);
        assertEquals(10,       r.departmentId());
        assertEquals(3,        r.headCount());
        assertEquals(270_000,  r.totalSalary(),   0.001);
        assertEquals(90_000,   r.averageSalary(),  0.001);
        assertEquals(80_000,   r.minSalary(),      0.001);
        assertEquals(100_000,  r.maxSalary(),      0.001);
    }

    @Test
    @DisplayName("two records with identical fields are equal (record equals)")
    void equals_sameFields_areEqual() {
        var a = new DepartmentSalaryReport(10, 3, 270_000, 90_000, 80_000, 100_000);
        var b = new DepartmentSalaryReport(10, 3, 270_000, 90_000, 80_000, 100_000);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("two records with different departmentId are not equal")
    void equals_differentDeptId_notEqual() {
        var a = new DepartmentSalaryReport(10, 3, 270_000, 90_000, 80_000, 100_000);
        var b = new DepartmentSalaryReport(20, 3, 270_000, 90_000, 80_000, 100_000);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("toString() contains departmentId and headCount")
    void toString_containsKeyFields() {
        var r = new DepartmentSalaryReport(10, 3, 270_000, 90_000, 80_000, 100_000);
        String s = r.toString();
        assertTrue(s.contains("10"));
        assertTrue(s.contains("3"));
    }

    @Test
    @DisplayName("single-employee department: min == max == average == salary")
    void singleEmployee_minMaxAverageEqual() {
        var r = new DepartmentSalaryReport(1, 1, 50_000, 50_000, 50_000, 50_000);
        assertEquals(r.minSalary(), r.maxSalary(), 0.001);
        assertEquals(r.minSalary(), r.averageSalary(), 0.001);
    }

    // ── Validation guards ─────────────────────────────────────────────────────

    @Test
    @DisplayName("departmentId <= 0 throws IllegalArgumentException")
    void validation_departmentIdZero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(0, 1, 50_000, 50_000, 50_000, 50_000));
    }

    @Test
    @DisplayName("negative headCount throws IllegalArgumentException")
    void validation_negativeHeadCount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, -1, 50_000, 50_000, 50_000, 50_000));
    }

    @Test
    @DisplayName("negative totalSalary throws IllegalArgumentException")
    void validation_negativeTotalSalary_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, 1, -1, 50_000, 50_000, 50_000));
    }

    @Test
    @DisplayName("negative averageSalary throws IllegalArgumentException")
    void validation_negativeAverageSalary_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, 1, 50_000, -1, 50_000, 50_000));
    }

    @Test
    @DisplayName("negative minSalary throws IllegalArgumentException")
    void validation_negativeMinSalary_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, 1, 50_000, 50_000, -1, 50_000));
    }

    @Test
    @DisplayName("negative maxSalary throws IllegalArgumentException")
    void validation_negativeMaxSalary_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, 1, 50_000, 50_000, 50_000, -1));
    }

    @Test
    @DisplayName("minSalary > maxSalary throws IllegalArgumentException")
    void validation_minExceedsMax_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepartmentSalaryReport(1, 1, 50_000, 50_000, 90_000, 80_000));
    }

    @Test
    @DisplayName("zero salary values are allowed (e.g. unpaid intern)")
    void validation_zeroSalariesAllowed() {
        assertDoesNotThrow(
                () -> new DepartmentSalaryReport(1, 1, 0, 0, 0, 0));
    }
}

