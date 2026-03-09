package com.example.helloworld.exception;

/**
 * Global Exception Handler — centralised mapper for the entire exception hierarchy.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Single place to translate every {@link EmployeeException} subtype into a
 *       human-readable error response.</li>
 *   <li>Callers (controllers, CLI entry-points) delegate here instead of
 *       repeating catch-blocks everywhere.</li>
 *   <li>Easily extensible: add a new {@code case} when a new exception subtype
 *       is introduced — the sealed hierarchy makes the switch exhaustive.</li>
 * </ul>
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * EmployeeException  (checked base)
 * ├── DuplicateEmployeeException   — id already exists
 * ├── DuplicateEmailException      — email already taken
 * ├── EmployeeNotFoundException    — id / email not found
 * ├── DepartmentNotFoundException  — departmentId unknown
 * ├── InvalidEmployeeDataException — field value breaks a data rule (e.g. min > max)
 * ├── ValidationException          — field value breaks a business rule
 * └── PayrollException             — payroll calculation failure
 * </pre>
 */
public final class GlobalExceptionHandler {

    private GlobalExceptionHandler() { /* utility — no instances */ }

    // ── Error response ────────────────────────────────────────────────────────

    /**
     * Immutable error response returned to every caller.
     *
     * @param errorCode  short machine-readable code (e.g. "DUPLICATE_EMAIL")
     * @param message    human-readable description
     * @param field      the offending field name, or {@code null} when not applicable
     */
    public record ErrorResponse(String errorCode, String message, String field) {

        /** Convenience factory when no specific field is involved. */
        public static ErrorResponse of(String errorCode, String message) {
            return new ErrorResponse(errorCode, message, null);
        }

        @Override
        public String toString() {
            return field != null
                    ? String.format("ErrorResponse{code='%s', field='%s', message='%s'}", errorCode, field, message)
                    : String.format("ErrorResponse{code='%s', message='%s'}", errorCode, message);
        }
    }

    // ── Main dispatch ─────────────────────────────────────────────────────────

    /**
     * Maps any {@link EmployeeException} subtype to a structured {@link ErrorResponse}.
     *
     * <p>Uses {@code instanceof} pattern matching so each branch has full access
     * to the typed exception without an extra cast.
     *
     * @param ex the exception to handle (must not be {@code null})
     * @return a non-null {@link ErrorResponse} describing the error
     */
    public static ErrorResponse handle(EmployeeException ex) {
        if (ex == null) throw new IllegalArgumentException("exception must not be null");

        if (ex instanceof DuplicateEmployeeException dee) {
            return new ErrorResponse(
                    "DUPLICATE_EMPLOYEE_ID",
                    dee.getMessage(),
                    "id"
            );
        }

        if (ex instanceof DuplicateEmailException dee) {
            return new ErrorResponse(
                    "DUPLICATE_EMAIL",
                    dee.getMessage(),
                    "email"
            );
        }

        if (ex instanceof DepartmentNotFoundException dnfe) {
            return new ErrorResponse(
                    "DEPARTMENT_NOT_FOUND",
                    dnfe.getMessage(),
                    "departmentId"
            );
        }

        if (ex instanceof EmployeeNotFoundException enfe) {
            return new ErrorResponse(
                    "EMPLOYEE_NOT_FOUND",
                    enfe.getMessage(),
                    null
            );
        }

        if (ex instanceof ValidationException ve) {
            return new ErrorResponse(
                    "VALIDATION_ERROR",
                    ve.getMessage(),
                    ve.getFieldName()
            );
        }

        if (ex instanceof InvalidEmployeeDataException ide) {
            return new ErrorResponse(
                    "INVALID_DATA",
                    ide.getMessage(),
                    ide.getFieldName()
            );
        }

        if (ex instanceof PayrollException pe) {
            return new ErrorResponse(
                    "PAYROLL_ERROR",
                    pe.getMessage(),
                    null
            );
        }

        // Fallback for any future EmployeeException subtypes not yet mapped
        return ErrorResponse.of("EMPLOYEE_ERROR", ex.getMessage());
    }

    /**
     * Convenience overload that also logs the mapped response to {@code System.err}.
     *
     * @param ex     the exception to handle
     * @param source label for the originating component (e.g. "EmployeeController")
     * @return the same {@link ErrorResponse} as {@link #handle(EmployeeException)}
     */
    public static ErrorResponse handleAndLog(EmployeeException ex, String source) {
        ErrorResponse response = handle(ex);
        System.err.printf("[%s] %s%n", source, response);
        return response;
    }
}

