package com.example.helloworld;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.domain.*;
import com.example.helloworld.factory.ApplicationFactory;
import com.example.helloworld.factory.InMemoryApplicationFactory;
import com.example.helloworld.repository.DepartmentKey;
import com.example.helloworld.repository.EmployeeKey;

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
        System.out.println("Lookup with k3 (new object, same data): " + empMap.get(k3));
        System.out.println("Map size (no duplicates): " + empMap.size());

        EmployeeKey k4 = new EmployeeKey(1, "alice.new@example.com");
        System.out.println("k1.equals(k4) same id, diff email: " + k1.equals(k4));

        // ── DepartmentKey : Record key (equals + hashCode auto-generated) ───
        printSection("DepartmentKey — Record as HashMap key");

        Map<DepartmentKey, String> deptMap = new HashMap<>();
        DepartmentKey dk1 = new DepartmentKey(10);
        DepartmentKey dk2 = new DepartmentKey(20);
        DepartmentKey dk3 = new DepartmentKey(10); // same as dk1

        deptMap.put(dk1, "Floor 3, Block A");
        deptMap.put(dk2, "Floor 1, Block B");

        System.out.println("dk1 hashCode : " + dk1.hashCode());
        System.out.println("dk3 hashCode : " + dk3.hashCode() + "  ← same as dk1");
        System.out.println("dk1.equals(dk3): " + dk1.equals(dk3));
        System.out.println("Lookup with dk3 (new record, same data): " + deptMap.get(dk3));
        System.out.println("Map size (no duplicates): " + deptMap.size());

        DepartmentKey dk4 = new DepartmentKey(10);
        System.out.println("dk1.equals(dk4) same id: " + dk1.equals(dk4));

        // ════════════════════════════════════════════════════════════════════
        // PART 2 — Controller → Service → Repository (layered architecture)
        //
        // Abstract Factory pattern: the entire stack is wired by one factory.
        // Swapping to a DB-backed stack only requires changing the factory.
        // ════════════════════════════════════════════════════════════════════

        ApplicationFactory factory  = new InMemoryApplicationFactory();           // Abstract Factory
        EmployeeController empCtrl  = factory.createEmployeeController();
        PayrollController  payCtrl  = factory.createPayrollController();

        // ── Factory Method + Builder pattern: create employees via EmployeeFactory ──
        // EmployeeFactory.create*() methods hide which constructor / Builder
        // is invoked; callers only depend on the Employee abstraction.

        Employee alice = EmployeeFactory.createPermanentEmployee(
                1, "Alice Kumar", "alice@example.com", 10, "Engineer",
                85_000, LocalDate.of(2020, 6, 1), true);

        Employee bob = EmployeeFactory.createPermanentEmployee(
                2, "Bob Singh", "bob@example.com", 10, "Engineer",
                90_000, LocalDate.of(2019, 3, 15), true);

        Employee carol = EmployeeFactory.createContractEmployee(
                3, "Carol Menon", "carol@example.com", 20, "Designer",
                60_000, LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));

        Employee dave = EmployeeFactory.createPermanentEmployee(
                4, "Dave Patel", "dave@example.com", 20, "Manager",
                110_000, LocalDate.of(2017, 8, 20), true);

        // INACTIVE employee — demonstrate Builder used directly for non-ACTIVE status
        Employee eve = ContractEmployee.builder()
                .id(5).name("Eve Sharma").email("eve@example.com")
                .departmentId(30).role("QA Analyst").salary(55_000)
                .status(EmployeeStatus.INACTIVE)
                .joiningDate(LocalDate.of(2022, 5, 10))
                .contractEndDate(LocalDate.of(2024, 5, 9))
                .build();

        printSection("Add employees via Controller (validation + service + repository)");
        empCtrl.addEmployee(alice);
        empCtrl.addEmployee(bob);
        empCtrl.addEmployee(carol);   // expired contract  — controller rejects via ValidationService
        empCtrl.addEmployee(dave);
        empCtrl.addEmployee(eve);     // INACTIVE          — controller rejects via ValidationService

        printSection("All Employees (" + empCtrl.countEmployees() + ")");
        empCtrl.getAllEmployees().forEach(System.out::println);

        printSection("Find by id = 3");
        empCtrl.getById(3).ifPresent(System.out::println);

        printSection("Find by email = dave@example.com");
        empCtrl.getByEmail("dave@example.com").ifPresent(System.out::println);

        printSection("Employees in Department 10");
        empCtrl.getByDepartment(10).forEach(System.out::println);

        printSection("INACTIVE Employees");
        empCtrl.getByStatus(EmployeeStatus.INACTIVE).forEach(System.out::println);

        printSection("Employees with role containing 'engineer'");
        empCtrl.getByRole("engineer").forEach(System.out::println);

        printSection("Salary range 60,000 – 95,000");
        empCtrl.getBySalaryRange(60_000, 95_000).forEach(System.out::println);

        printSection("Aggregations");
        System.out.printf("  Total employees : %d%n",   empCtrl.countEmployees());
        System.out.printf("  Total salary    : %.2f%n", empCtrl.totalSalary());
        System.out.printf("  Average salary  : %.2f%n", empCtrl.averageSalary());

        printSection("Update Alice's salary to 95,000 via Controller");
        alice.setSalary(95_000);
        empCtrl.updateEmployee(alice);
        empCtrl.getById(1).ifPresent(System.out::println);

        printSection("Remove Bob (id=2) via Controller");
        empCtrl.removeEmployee(2);
        System.out.println("Store size after removal : " + empCtrl.countEmployees());
        System.out.println("Find Bob by id           : " + empCtrl.getById(2));

        // ═════════════════════════════════════════════════════════════���══════
        // PART 3 — Controller error-handling demo
        // ════════════════════════════════════════════════════════════════════

        printSection("Controller handles duplicate employee — no exception thrown to caller");
        empCtrl.addEmployee(alice);       // already exists — handled internally

        printSection("Controller handles duplicate email — no exception thrown to caller");
        Employee sameEmail = PermanentEmployee.builder()
                .id(99).name("Alice Twin").email("alice@example.com")
                .departmentId(10).role("Engineer").salary(80_000)
                .joiningDate(LocalDate.of(2021, 1, 1))
                .gratuityEligible(true)
                .build();
        empCtrl.addEmployee(sameEmail);

        printSection("Controller handles remove of non-existent id — no exception thrown to caller");
        empCtrl.removeEmployee(999);

        printSection("Controller handles invalid salary range — returns empty list, no exception");
        List<Employee> badRange = empCtrl.getBySalaryRange(-1000, 90_000);
        System.out.println("Result size: " + badRange.size());

        printSection("Controller handles ValidationException (invalid email) — no exception to caller");
        Employee badEmailEmp = PermanentEmployee.builder()
                .id(50).name("Bad Email").email("notanemail")
                .departmentId(10).role("Engineer").salary(70_000)
                .joiningDate(LocalDate.of(2022, 1, 1))
                .gratuityEligible(true)
                .build();
        empCtrl.addEmployee(badEmailEmp);

        // ════════════════════════════════════════════════════════════════════
        // PART 4 — PayrollController → PayrollService → Strategy
        //          PayrollService uses the Singleton PayrollStrategyRegistry
        // ════════════════════════════════════════════════════════════════════

        LocalDate payrollMonth = LocalDate.of(2026, 3, 1);

        // Factory Method creates validated employees; Builder used for fine-grained control
        Employee validEmp = EmployeeFactory.createPermanentEmployee(
                10, "Sara Ali", "sara@example.com", 10, "Engineer",
                80_000, LocalDate.of(2021, 4, 1), true);

        Employee activeContract = EmployeeFactory.createContractEmployee(
                14, "Lisa Chen", "lisa@example.com", 20, "Designer",
                60_000, LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));

        Employee expiredContract = EmployeeFactory.createContractEmployee(
                12, "Raj Kumar", "raj@example.com", 20, "Designer",
                50_000, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 30));

        printSection("PayrollController — permanent employee (tax 20%)");
        PayrollRecord pr1 = payCtrl.processPayroll(1, validEmp, payrollMonth);
        if (pr1 != null) {
            System.out.printf("  Employee : %s%n",    validEmp.getName());
            System.out.printf("  Gross    : %.2f%n",  pr1.grossSalary());
            System.out.printf("  Tax (20%%): %.2f%n", pr1.taxAmount());
            System.out.printf("  Net      : %.2f%n",  pr1.netSalary());
        }

        printSection("PayrollController — contract employee (tax 10%)");
        PayrollRecord pr2 = payCtrl.processPayroll(2, activeContract, payrollMonth);
        if (pr2 != null) {
            System.out.printf("  Employee : %s%n",    activeContract.getName());
            System.out.printf("  Gross    : %.2f%n",  pr2.grossSalary());
            System.out.printf("  Tax (10%%): %.2f%n", pr2.taxAmount());
            System.out.printf("  Net      : %.2f%n",  pr2.netSalary());
        }

        printSection("PayrollController — expired contract (error handled, returns null)");
        PayrollRecord pr3 = payCtrl.processPayroll(3, expiredContract, payrollMonth);
        System.out.println("Result for expired contract: " + pr3);

        printSection("PayrollController.processAll — mixed batch (expired skipped)");
        List<Employee> batch = List.of(validEmp, activeContract, expiredContract);
        List<PayrollRecord> records = payCtrl.processAll(batch, payrollMonth);
        records.forEach(r -> System.out.printf("  id=%-2d  employeeId=%-2d  net=%.2f%n",
                r.id(), r.employeeId(), r.netSalary()));
    }

    private static void printSection(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 55 - title.length())));
    }
}
