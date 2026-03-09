package com.example.helloworld.exception;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GlobalExceptionHandler — verifies that every exception subtype
 * in the hierarchy is mapped to the correct error code and carries the
 * right field name.
 *
 * Exception hierarchy under test:
 *
 *   EmployeeException  (base)
 *   ├── DuplicateEmployeeException   → DUPLICATE_EMPLOYEE_ID  (field=id)
 *   ├── DuplicateEmailException      → DUPLICATE_EMAIL        (field=email)
 *   ├── EmployeeNotFoundException    → EMPLOYEE_NOT_FOUND     (field=null)
 *   ├── DepartmentNotFoundException  → DEPARTMENT_NOT_FOUND   (field=departmentId)
 *   ├── ValidationException          → VALIDATION_ERROR       (field=fieldName)
 *   ├── InvalidEmployeeDataException → INVALID_DATA           (field=fieldName)
 *   └── PayrollException             → PAYROLL_ERROR          (field=null)
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    // ── DuplicateEmployeeException ────────────────────────────────────────────

    @Test
    @DisplayName("handle — DuplicateEmployeeException → DUPLICATE_EMPLOYEE_ID, field=id")
    void handle_duplicateEmployee_correctCodeAndField() {
        var response = GlobalExceptionHandler.handle(new DuplicateEmployeeException(42));
        assertEquals("DUPLICATE_EMPLOYEE_ID", response.errorCode());
        assertEquals("id", response.field());
        assertNotNull(response.message());
        assertTrue(response.message().contains("42"));
    }

    // ── DuplicateEmailException ───────────────────────────────────────────────

    @Test
    @DisplayName("handle — DuplicateEmailException → DUPLICATE_EMAIL, field=email")
    void handle_duplicateEmail_correctCodeAndField() {
        var response = GlobalExceptionHandler.handle(new DuplicateEmailException("alice@example.com"));
        assertEquals("DUPLICATE_EMAIL", response.errorCode());
        assertEquals("email", response.field());
        assertTrue(response.message().contains("alice@example.com"));
    }

    // ── DepartmentNotFoundException ───────────────────────────────────────────

    @Test
    @DisplayName("handle — DepartmentNotFoundException → DEPARTMENT_NOT_FOUND, field=departmentId")
    void handle_departmentNotFound_correctCodeAndField() {
        var response = GlobalExceptionHandler.handle(new DepartmentNotFoundException(99));
        assertEquals("DEPARTMENT_NOT_FOUND", response.errorCode());
        assertEquals("departmentId", response.field());
        assertTrue(response.message().contains("99"));
    }

    // ── EmployeeNotFoundException ─────────────────────────────────────────────

    @Test
    @DisplayName("handle — EmployeeNotFoundException(int) → EMPLOYEE_NOT_FOUND, field=null")
    void handle_employeeNotFoundById_correctCode() {
        var response = GlobalExceptionHandler.handle(new EmployeeNotFoundException(7));
        assertEquals("EMPLOYEE_NOT_FOUND", response.errorCode());
        assertNull(response.field());
        assertTrue(response.message().contains("7"));
    }

    @Test
    @DisplayName("handle — EmployeeNotFoundException(String) → EMPLOYEE_NOT_FOUND, field=null")
    void handle_employeeNotFoundByEmail_correctCode() {
        var response = GlobalExceptionHandler.handle(new EmployeeNotFoundException("x@y.com"));
        assertEquals("EMPLOYEE_NOT_FOUND", response.errorCode());
        assertNull(response.field());
    }

    // ── ValidationException ───────────────────────────────────────────────────

    @Test
    @DisplayName("handle — ValidationException → VALIDATION_ERROR, field=fieldName")
    void handle_validationException_correctCodeAndField() {
        var response = GlobalExceptionHandler.handle(
                new ValidationException("salary", -100, "must not be negative"));
        assertEquals("VALIDATION_ERROR", response.errorCode());
        assertEquals("salary", response.field());
        assertTrue(response.message().contains("salary"));
    }

    // ── InvalidEmployeeDataException ──────────────────────────────────────────

    @Test
    @DisplayName("handle — InvalidEmployeeDataException → INVALID_DATA, field=fieldName")
    void handle_invalidData_correctCodeAndField() {
        var response = GlobalExceptionHandler.handle(
                new InvalidEmployeeDataException("salaryRange", "-100..50", "min must not be negative"));
        assertEquals("INVALID_DATA", response.errorCode());
        assertEquals("salaryRange", response.field());
    }

    // ── PayrollException ──────────────────────────────────────────────────────

    @Test
    @DisplayName("handle — PayrollException → PAYROLL_ERROR, field=null")
    void handle_payrollException_correctCode() {
        var response = GlobalExceptionHandler.handle(new PayrollException(5, "expired contract"));
        assertEquals("PAYROLL_ERROR", response.errorCode());
        assertNull(response.field());
        assertTrue(response.message().contains("5"));
    }

    // ── Null guard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("handle — null throws IllegalArgumentException")
    void handle_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> GlobalExceptionHandler.handle(null));
    }

    // ── ErrorResponse record ──────────────────────────────────────────────────

    @Test
    @DisplayName("ErrorResponse.of — creates response with null field")
    void errorResponse_of_nullField() {
        var r = GlobalExceptionHandler.ErrorResponse.of("MY_CODE", "something went wrong");
        assertEquals("MY_CODE", r.errorCode());
        assertEquals("something went wrong", r.message());
        assertNull(r.field());
    }

    @Test
    @DisplayName("ErrorResponse — toString includes code and message (no field)")
    void errorResponse_toString_noField() {
        var r = GlobalExceptionHandler.ErrorResponse.of("CODE", "msg");
        assertTrue(r.toString().contains("CODE"));
        assertTrue(r.toString().contains("msg"));
    }

    @Test
    @DisplayName("ErrorResponse — toString includes field when present")
    void errorResponse_toString_withField() {
        var r = new GlobalExceptionHandler.ErrorResponse("CODE", "msg", "email");
        assertTrue(r.toString().contains("email"));
    }

    // ── Parameterised: code mapping table ────────────────────────────────────

    @ParameterizedTest(name = "exception={0}  expectedCode={1}")
    @DisplayName("handle — parameterised error-code mapping")
    @CsvSource({
            "DUPLICATE_EMPLOYEE, DUPLICATE_EMPLOYEE_ID",
            "DUPLICATE_EMAIL,    DUPLICATE_EMAIL",
            "DEPT_NOT_FOUND,     DEPARTMENT_NOT_FOUND",
            "EMP_NOT_FOUND,      EMPLOYEE_NOT_FOUND",
            "VALIDATION,         VALIDATION_ERROR",
            "INVALID_DATA,       INVALID_DATA",
            "PAYROLL,            PAYROLL_ERROR"
    })
    void handle_parameterisedCodeMapping(String exceptionKey, String expectedCode) {
        EmployeeException ex = switch (exceptionKey) {
            case "DUPLICATE_EMPLOYEE" -> new DuplicateEmployeeException(1);
            case "DUPLICATE_EMAIL"    -> new DuplicateEmailException("a@b.com");
            case "DEPT_NOT_FOUND"     -> new DepartmentNotFoundException(10);
            case "EMP_NOT_FOUND"      -> new EmployeeNotFoundException(1);
            case "VALIDATION"         -> new ValidationException("f", "v", "r");
            case "INVALID_DATA"       -> new InvalidEmployeeDataException("f", "v", "r");
            case "PAYROLL"            -> new PayrollException(1, "reason");
            default -> throw new IllegalArgumentException("unknown key: " + exceptionKey);
        };
        assertEquals(expectedCode, GlobalExceptionHandler.handle(ex).errorCode());
    }

    // ── handleAndLog ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleAndLog — returns same ErrorResponse as handle()")
    void handleAndLog_returnsSameResponse() {
        var ex = new DuplicateEmailException("z@z.com");
        var direct  = GlobalExceptionHandler.handle(ex);
        var logged  = GlobalExceptionHandler.handleAndLog(ex, "TestSource");
        assertEquals(direct.errorCode(), logged.errorCode());
        assertEquals(direct.field(),     logged.field());
    }
}

