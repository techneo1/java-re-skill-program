package com.example.helloworld;

import com.example.helloworld.exception.*;
import com.example.helloworld.model.*;
import com.example.helloworld.store.DepartmentKey;
import com.example.helloworld.store.EmployeeKey;
import com.example.helloworld.store.EmployeeStore;
import com.example.helloworld.store.InMemoryEmployeeStore;

import java.time.LocalDate;
import java.util.HashMap;
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
        // PART 2 — InMemoryEmployeeStore (backed by EmployeeKey + DepartmentKey)
        // ════════════════════════════════════════════════════════════════════

        EmployeeStore store = new InMemoryEmployeeStore();

        Employee alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com", 10, "Engineer",    85_000, EmployeeStatus.ACTIVE,   LocalDate.of(2020, 6,  1),  true);
        Employee bob   = new PermanentEmployee(2, "Bob Singh",   "bob@example.com",   10, "Engineer",    90_000, EmployeeStatus.ACTIVE,   LocalDate.of(2019, 3, 15),  true);
        Employee carol = new ContractEmployee (3, "Carol Menon", "carol@example.com", 20, "Designer",    60_000, EmployeeStatus.ACTIVE,   LocalDate.of(2023, 1,  1),  LocalDate.of(2025, 12, 31));
        Employee dave  = new PermanentEmployee(4, "Dave Patel",  "dave@example.com",  20, "Manager",    110_000, EmployeeStatus.ACTIVE,   LocalDate.of(2017, 8, 20),  true);
        Employee eve   = new ContractEmployee (5, "Eve Sharma",  "eve@example.com",   30, "QA Analyst",  55_000, EmployeeStatus.INACTIVE, LocalDate.of(2022, 5, 10),  LocalDate.of(2024, 5, 9));

        store.add(alice); store.add(bob); store.add(carol); store.add(dave); store.add(eve);

        printSection("All Employees (" + store.count() + ")");
        store.findAll().forEach(System.out::println);

        printSection("Find by id = 3");
        store.findById(3).ifPresent(System.out::println);

        printSection("Find by email = dave@example.com");
        store.findByEmail("dave@example.com").ifPresent(System.out::println);

        printSection("Employees in Department 10");
        store.findByDepartment(10).forEach(System.out::println);

        printSection("INACTIVE Employees");
        store.findByStatus(EmployeeStatus.INACTIVE).forEach(System.out::println);

        printSection("Employees with role containing 'engineer'");
        store.findByRole("engineer").forEach(System.out::println);

        printSection("Salary range 60,000 – 95,000");
        store.findBySalaryRange(60_000, 95_000).forEach(System.out::println);

        printSection("Aggregations");
        System.out.printf("  Total employees : %d%n",   store.count());
        System.out.printf("  Total salary    : %.2f%n", store.totalSalary());
        System.out.printf("  Average salary  : %.2f%n", store.averageSalary());

        printSection("Update Alice's salary to 95,000");
        alice.setSalary(95_000);
        store.update(alice);
        store.findById(1).ifPresent(System.out::println);

        printSection("Remove Eve (id=5)");
        store.remove(5);
        System.out.println("Store size after removal : " + store.count());
        System.out.println("Find Eve by id           : " + store.findById(5));

        // ════════════════════════════════════════════════════════════════════
        // PART 3 — Custom exception demo
        // ════════════════════════════════════════════════════════════════════

        EmployeeStore exStore = new InMemoryEmployeeStore();
        Employee emp = new PermanentEmployee(1, "Alice Kumar", "alice@example.com", 10,
                "Engineer", 85_000, EmployeeStatus.ACTIVE, LocalDate.of(2020, 6, 1), true);
        exStore.add(emp);

        // ── DuplicateEmployeeException ────────────────────────────────────
        printSection("DuplicateEmployeeException — add same id twice");
        try {
            Employee duplicate = new PermanentEmployee(1, "Alice Copy", "alice.copy@example.com",
                    10, "Engineer", 80_000, EmployeeStatus.ACTIVE, LocalDate.of(2021, 1, 1), true);
            exStore.add(duplicate);
        } catch (DuplicateEmployeeException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Duplicate id: " + e.getDuplicateId());
        }

        // ── DuplicateEmailException ───────────────────────────────────────
        printSection("DuplicateEmailException — add different id but same email");
        try {
            Employee sameEmail = new PermanentEmployee(2, "Alice Twin", "alice@example.com",
                    10, "Engineer", 80_000, EmployeeStatus.ACTIVE, LocalDate.of(2021, 1, 1), true);
            exStore.add(sameEmail);
        } catch (DuplicateEmailException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Duplicate email: " + e.getDuplicateEmail());
        }

        // ── EmployeeNotFoundException (remove) ────────────────────────────
        printSection("EmployeeNotFoundException — remove non-existent id");
        try {
            exStore.remove(999);
        } catch (EmployeeNotFoundException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Search key: " + e.getSearchKey());
        }

        // ── EmployeeNotFoundException (update) ────────────────────────────
        printSection("EmployeeNotFoundException — update non-existent employee");
        try {
            Employee ghost = new PermanentEmployee(404, "Ghost User", "ghost@example.com",
                    10, "Engineer", 50_000, EmployeeStatus.INACTIVE, LocalDate.of(2020, 1, 1), false);
            exStore.update(ghost);
        } catch (EmployeeNotFoundException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        // ── InvalidEmployeeDataException ──────────────────────────────────
        printSection("InvalidEmployeeDataException — negative min salary in range query");
        try {
            exStore.findBySalaryRange(-1000, 90_000);
        } catch (InvalidEmployeeDataException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Field  : " + e.getFieldName());
            System.out.println("Value  : " + e.getRejectedValue());
        }

        printSection("InvalidEmployeeDataException — min > max in range query");
        try {
            exStore.findBySalaryRange(90_000, 50_000);
        } catch (InvalidEmployeeDataException e) {
            System.out.println("Caught : " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
        }

        // ── Catching via base EmployeeException ───────────────────────────
        printSection("Catching all via base EmployeeException");
        try {
            exStore.remove(777);
        } catch (EmployeeException e) {
            System.out.println("Caught as base type : " + e.getClass().getSimpleName());
            System.out.println("Message             : " + e.getMessage());
        }
    }

    private static void printSection(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 55 - title.length())));
    }
}
