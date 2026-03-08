package com.example.helloworld;

import com.example.helloworld.exception.*;
import com.example.helloworld.domain.*;
import com.example.helloworld.repository.DepartmentKey;
import com.example.helloworld.repository.EmployeeKey;
import com.example.helloworld.repository.EmployeeRepository;
import com.example.helloworld.repository.inmemory.InMemoryEmployeeRepository;
import com.example.helloworld.service.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {

        // ════════════════════════════════════════════════════════════════════
        // PART 1 — Custom object as HashMap key (standalone demo)
        // ════════════════════════════════════════════════════════════════════

        // ── EmployeeKey : manual class key (equals + hashCode hand-written) ─
        printSection("EmployeeKey — custom class as HashMap key");

        Map<EmployeeKey, String> empMap = new HashMap<>();
        EmployeeKey k1 = new EmployeeKey(1, "alice@example.com");
        EmployeeKey k2 = new EmployeeKey(2, "bob@example.com");
        EmployeeKey k3 = new EmployeeKey(1, "alice@example.com"); // same as k1

        empMap.put(k1, "Alice Kumar");
        empMap.put(k2, "Bob Singh");

        System.out.println("k1 hashCode : " + k1.hashCode());
        System.out.println("k3 hashCode : " + k3.hashCode() + "  ← same as k1 (equal keys must have equal hashes)");
        System.out.println("k1.equals(k3): " + k1.equals(k3) + "  ← logically equal");
        System.out.println("k1 == k3    : " + (k1 == k3)     + "  ← different references");
        System.out.println("Lookup with k3 (new object, same data): " + empMap.get(k3)); // must find "Alice Kumar"
        System.out.println("Map size (no duplicates): " + empMap.size());

        // Demonstrate what happens with a DIFFERENT email for the same id — not equal
        EmployeeKey k4 = new EmployeeKey(1, "alice.new@example.com");
        System.out.println("k1.equals(k4) same id, diff email: " + k1.equals(k4)); // false

        // ── DepartmentKey : Record key (equals + hashCode auto-generated) ───
        printSection("DepartmentKey — Record as HashMap key");

        Map<DepartmentKey, String> deptMap = new HashMap<>();
        DepartmentKey dk1 = new DepartmentKey(10, "Engineering");
        DepartmentKey dk2 = new DepartmentKey(20, "Design");
        DepartmentKey dk3 = new DepartmentKey(10, "Engineering"); // same as dk1

        deptMap.put(dk1, "Floor 3, Block A");
        deptMap.put(dk2, "Floor 1, Block B");

        System.out.println("dk1 hashCode : " + dk1.hashCode());
        System.out.println("dk3 hashCode : " + dk3.hashCode() + "  ← same as dk1");
        System.out.println("dk1.equals(dk3): " + dk1.equals(dk3));
        System.out.println("Lookup with dk3 (new record, same data): " + deptMap.get(dk3)); // must find location
        System.out.println("Map size (no duplicates): " + deptMap.size());

        // Records normalise the name to lowercase in compact constructor
        DepartmentKey dk4 = new DepartmentKey(10, "ENGINEERING");  // uppercase — normalised to same
        System.out.println("dk1.equals(dk4) case-insensitive match: " + dk1.equals(dk4));

        // ════════════════════════════════════════════════════════════════════
        // PART 2 — EmployeeService (service → repository → in-memory)
        // ════════════════════════════════════════════════════════════════════

        EmployeeRepository repository = new InMemoryEmployeeRepository();
        EmployeeService service = new EmployeeServiceImpl(repository);

        Employee alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com", 10, "Engineer",    85_000, EmployeeStatus.ACTIVE,   LocalDate.of(2020, 6,  1),  true);
        Employee bob   = new PermanentEmployee(2, "Bob Singh",   "bob@example.com",   10, "Engineer",    90_000, EmployeeStatus.ACTIVE,   LocalDate.of(2019, 3, 15),  true);
        Employee carol = new ContractEmployee (3, "Carol Menon", "carol@example.com", 20, "Designer",    60_000, EmployeeStatus.ACTIVE,   LocalDate.of(2023, 1,  1),  LocalDate.of(2025, 12, 31));
        Employee dave  = new PermanentEmployee(4, "Dave Patel",  "dave@example.com",  20, "Manager",    110_000, EmployeeStatus.ACTIVE,   LocalDate.of(2017, 8, 20),  true);
        Employee eve   = new ContractEmployee (5, "Eve Sharma",  "eve@example.com",   30, "QA Analyst",  55_000, EmployeeStatus.INACTIVE, LocalDate.of(2022, 5, 10),  LocalDate.of(2024, 5, 9));

        service.addEmployee(alice); service.addEmployee(bob); service.addEmployee(carol);
        service.addEmployee(dave);  service.addEmployee(eve);

        printSection("All Employees (" + service.countEmployees() + ")");
        service.getAllEmployees().forEach(System.out::println);

        printSection("Find by id = 3");
        service.getById(3).ifPresent(System.out::println);

        printSection("Find by email = dave@example.com");
        service.getByEmail("dave@example.com").ifPresent(System.out::println);

        printSection("Employees in Department 10");
        service.getByDepartment(10).forEach(System.out::println);

        printSection("INACTIVE Employees");
        service.getByStatus(EmployeeStatus.INACTIVE).forEach(System.out::println);

        printSection("Employees with role containing 'engineer'");
        service.getByRole("engineer").forEach(System.out::println);

        printSection("Salary range 60,000 – 95,000");
        service.getBySalaryRange(60_000, 95_000).forEach(System.out::println);

        printSection("Aggregations");
        System.out.printf("  Total employees : %d%n",   service.countEmployees());
        System.out.printf("  Total salary    : %.2f%n", service.totalSalary());
        System.out.printf("  Average salary  : %.2f%n", service.averageSalary());

        printSection("Update Alice's salary to 95,000");
        alice.setSalary(95_000);
        service.updateEmployee(alice);
        service.getById(1).ifPresent(System.out::println);

        printSection("Remove Eve (id=5)");
        service.removeEmployee(5);
        System.out.println("Store size after removal : " + service.countEmployees());
        System.out.println("Find Eve by id           : " + service.getById(5));

        // ════════════════════════════════════════════════════════════════════
        // PART 3 — Custom exception demo
        // ════════════════════════════════════════════════════════════════════

        EmployeeService exService = new EmployeeServiceImpl(new InMemoryEmployeeRepository());
        Employee emp = new PermanentEmployee(1, "Alice Kumar", "alice@example.com", 10,
                "Engineer", 85_000, EmployeeStatus.ACTIVE, LocalDate.of(2020, 6, 1), true);
        exService.addEmployee(emp);

        printSection("DuplicateEmployeeException — add same id twice");
        try {
            Employee duplicate = new PermanentEmployee(1, "Alice Copy", "alice.copy@example.com",
                    10, "Engineer", 80_000, EmployeeStatus.ACTIVE, LocalDate.of(2021, 1, 1), true);
            exService.addEmployee(duplicate);
        } catch (DuplicateEmployeeException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Duplicate id: " + e.getDuplicateId());
        }

        printSection("DuplicateEmailException — add different id but same email");
        try {
            Employee sameEmail = new PermanentEmployee(2, "Alice Twin", "alice@example.com",
                    10, "Engineer", 80_000, EmployeeStatus.ACTIVE, LocalDate.of(2021, 1, 1), true);
            exService.addEmployee(sameEmail);
        } catch (DuplicateEmailException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Duplicate email: " + e.getDuplicateEmail());
        }

        printSection("EmployeeNotFoundException — remove non-existent id");
        try {
            exService.removeEmployee(999);
        } catch (EmployeeNotFoundException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Search key: " + e.getSearchKey());
        }

        printSection("EmployeeNotFoundException — update non-existent employee");
        try {
            Employee ghost = new PermanentEmployee(404, "Ghost User", "ghost@example.com",
                    10, "Engineer", 50_000, EmployeeStatus.INACTIVE, LocalDate.of(2020, 1, 1), false);
            exService.updateEmployee(ghost);
        } catch (EmployeeNotFoundException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        printSection("InvalidEmployeeDataException — negative min salary in range query");
        try {
            exService.getBySalaryRange(-1000, 90_000);
        } catch (InvalidEmployeeDataException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Field  : " + e.getFieldName());
            System.out.println("Value  : " + e.getRejectedValue());
        }

        printSection("InvalidEmployeeDataException — min > max in range query");
        try {
            exService.getBySalaryRange(90_000, 50_000);
        } catch (InvalidEmployeeDataException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        printSection("Catching all via base EmployeeException");
        try {
            exService.removeEmployee(777);
        } catch (EmployeeException e) {
            System.out.println("Caught as base type : " + e.getClass().getSimpleName());
            System.out.println("Message             : " + e.getMessage());
        }

        // ════════════════════════════════════════════════════════════════════
        // PART 4 — ValidationService + PayrollService + error handling
        // ════════════════════════════════════════════════════════════════════

        ValidationService validationService = new EmployeeValidationService();
        PayrollService    payrollService    = new PayrollServiceImpl();
        LocalDate         payrollMonth      = LocalDate.of(2026, 3, 1);

        // ── ValidationService ─────────────────────────────────────────────
        printSection("ValidationService — valid permanent employee");
        Employee validEmp = new PermanentEmployee(10, "Sara Ali", "sara@example.com",
                10, "Engineer", 80_000, EmployeeStatus.ACTIVE, LocalDate.of(2021, 4, 1), true);
        try {
            validationService.validate(validEmp);
            System.out.println("Validation passed for: " + validEmp.getName());
        } catch (ValidationException e) {
            System.out.println("Unexpected: " + e.getMessage());
        }

        printSection("ValidationException — INACTIVE employee");
        Employee inactiveEmp = new PermanentEmployee(11, "Joe Doe", "joe@example.com",
                10, "Analyst", 60_000, EmployeeStatus.INACTIVE, LocalDate.of(2020, 1, 1), false);
        try {
            validationService.validate(inactiveEmp);
        } catch (ValidationException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Field  : " + e.getFieldName());
            System.out.println("Value  : " + e.getRejectedValue());
        }

        printSection("ValidationException — expired contract employee");
        Employee expiredContract = new ContractEmployee(12, "Raj Kumar", "raj@example.com",
                20, "Designer", 50_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));
        try {
            validationService.validate(expiredContract);
        } catch (ValidationException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        printSection("ValidationException — invalid email (no @)");
        Employee badEmail = new PermanentEmployee(13, "Bad Email", "notanemail",
                10, "Engineer", 70_000, EmployeeStatus.ACTIVE, LocalDate.of(2022, 1, 1), true);
        try {
            validationService.validate(badEmail);
        } catch (ValidationException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        // ── PayrollService ────────────────────────────────────────────────
        printSection("PayrollService — permanent employee (tax 20%)");
        try {
            PayrollRecord pr = payrollService.processPayroll(1, validEmp, payrollMonth);
            System.out.printf("  Employee : %s%n",    validEmp.getName());
            System.out.printf("  Gross    : %.2f%n",  pr.grossSalary());
            System.out.printf("  Tax (20%%): %.2f%n", pr.taxAmount());
            System.out.printf("  Net      : %.2f%n",  pr.netSalary());
        } catch (PayrollException e) {
            System.out.println("Unexpected: " + e.getMessage());
        }

        printSection("PayrollService — contract employee (tax 10%)");
        Employee activeContract = new ContractEmployee(14, "Lisa Chen", "lisa@example.com",
                20, "Designer", 60_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));
        try {
            PayrollRecord pr = payrollService.processPayroll(2, activeContract, payrollMonth);
            System.out.printf("  Employee : %s%n",    activeContract.getName());
            System.out.printf("  Gross    : %.2f%n",  pr.grossSalary());
            System.out.printf("  Tax (10%%): %.2f%n", pr.taxAmount());
            System.out.printf("  Net      : %.2f%n",  pr.netSalary());
        } catch (PayrollException e) {
            System.out.println("Unexpected: " + e.getMessage());
        }

        printSection("PayrollException — expired contract employee");
        try {
            payrollService.processPayroll(3, expiredContract, payrollMonth);
        } catch (PayrollException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Employee id: " + e.getEmployeeId());
        }

        printSection("PayrollService.processAll — mixed batch (expired contract skipped)");
        List<Employee> batch = List.of(validEmp, activeContract, expiredContract);
        List<PayrollRecord> records = payrollService.processAll(batch, payrollMonth);
        System.out.println("Records processed: " + records.size() + " of " + batch.size());
        records.forEach(r -> System.out.printf("  id=%-2d  employeeId=%-2d  net=%.2f%n",
                r.id(), r.employeeId(), r.netSalary()));
    }

    private static void printSection(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 55 - title.length())));
    }
}
