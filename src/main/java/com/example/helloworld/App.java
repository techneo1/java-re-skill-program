package com.example.helloworld;

import com.example.helloworld.model.*;
import com.example.helloworld.store.EmployeeStore;
import com.example.helloworld.store.InMemoryEmployeeStore;

import java.time.LocalDate;

public class App {

    public static void main(String[] args) {

        EmployeeStore store = new InMemoryEmployeeStore();

        // ── Seed data ────────────────────────────────────────────────────────
        Employee alice = new PermanentEmployee(1, "Alice Kumar",   "alice@example.com",  10, "Engineer",       85_000, EmployeeStatus.ACTIVE,   LocalDate.of(2020, 6, 1),  true);
        Employee bob   = new PermanentEmployee(2, "Bob Singh",     "bob@example.com",    10, "Engineer",       90_000, EmployeeStatus.ACTIVE,   LocalDate.of(2019, 3, 15), true);
        Employee carol = new ContractEmployee (3, "Carol Menon",   "carol@example.com",  20, "Designer",       60_000, EmployeeStatus.ACTIVE,   LocalDate.of(2023, 1, 1),  LocalDate.of(2025, 12, 31));
        Employee dave  = new PermanentEmployee(4, "Dave Patel",    "dave@example.com",   20, "Manager",       110_000, EmployeeStatus.ACTIVE,   LocalDate.of(2017, 8, 20), true);
        Employee eve   = new ContractEmployee (5, "Eve Sharma",    "eve@example.com",    30, "QA Analyst",     55_000, EmployeeStatus.INACTIVE, LocalDate.of(2022, 5, 10), LocalDate.of(2024, 5, 9));

        store.add(alice);
        store.add(bob);
        store.add(carol);
        store.add(dave);
        store.add(eve);

        // ── Find all ─────────────────────────────────────────────────────────
        printSection("All Employees (" + store.count() + ")");
        store.findAll().forEach(System.out::println);

        // ── Find by id ───────────────────────────────────────────────────────
        printSection("Find by id = 3");
        store.findById(3).ifPresent(System.out::println);

        // ── Find by email ────────────────────────────────────────────────────
        printSection("Find by email = dave@example.com");
        store.findByEmail("dave@example.com").ifPresent(System.out::println);

        // ── Find by department ───────────────────────────────────────────────
        printSection("Employees in Department 10");
        store.findByDepartment(10).forEach(System.out::println);

        // ── Find by status ───────────────────────────────────────────────────
        printSection("INACTIVE Employees");
        store.findByStatus(EmployeeStatus.INACTIVE).forEach(System.out::println);

        // ── Find by role ─────────────────────────────────────────────────────
        printSection("Employees with role containing 'engineer'");
        store.findByRole("engineer").forEach(System.out::println);

        // ── Find by salary range ─────────────────────────────────────────────
        printSection("Employees with salary between 60,000 and 95,000");
        store.findBySalaryRange(60_000, 95_000).forEach(System.out::println);

        // ── Aggregations ─────────────────────────────────────────────────────
        printSection("Aggregations");
        System.out.printf("  Total employees : %d%n",     store.count());
        System.out.printf("  Total salary    : %.2f%n",   store.totalSalary());
        System.out.printf("  Average salary  : %.2f%n",   store.averageSalary());

        // ── Update ───────────────────────────────────────────────────────────
        printSection("Update Alice's salary to 95,000 and promote to Senior Engineer");
        alice.setSalary(95_000);
        store.update(alice);
        store.findById(1).ifPresent(System.out::println);

        // ── Remove ───────────────────────────────────────────────────────────
        printSection("Remove Eve (id=5)");
        store.remove(5);
        System.out.println("Store size after removal: " + store.count());
        System.out.println("Find Eve by id: " + store.findById(5));
    }

    private static void printSection(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 55 - title.length())));
    }
}
