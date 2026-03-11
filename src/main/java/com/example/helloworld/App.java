package com.example.helloworld;

import com.example.helloworld.controller.EmployeeController;
import com.example.helloworld.controller.PayrollController;
import com.example.helloworld.controller.SalaryAnalyticsController;
import com.example.helloworld.domain.*;
import com.example.helloworld.exception.*;
import com.example.helloworld.factory.ApplicationFactory;
import com.example.helloworld.factory.InMemoryApplicationFactory;
import com.example.helloworld.repository.DepartmentKey;
import com.example.helloworld.repository.EmployeeKey;
import com.example.helloworld.service.EmployeeValidationService;
import com.example.helloworld.service.PayrollStrategyRegistry;
import com.example.helloworld.domain.payroll.ContractEmployeePayrollStrategy;
import com.example.helloworld.domain.payroll.PermanentEmployeePayrollStrategy;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        ApplicationFactory         factory      = new InMemoryApplicationFactory();
        EmployeeController         empCtrl      = factory.createEmployeeController();
        PayrollController          payCtrl      = factory.createPayrollController();
        SalaryAnalyticsController  analyticsCtrl = factory.createSalaryAnalyticsController();

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

        // ════════════════════════════════════════════════════════════════════
        // PART 5 — Salary Analytics (stream pipelines)
        //
        // SalaryAnalyticsController → SalaryAnalyticsService (streams only)
        // ════════════════════════════════════════════════════════════════════

        printSection("Salary Analytics — group employees by department");
        analyticsCtrl.groupByDepartment().forEach((deptId, report) ->
                System.out.printf("  dept=%-3d  headCount=%-2d  avg=%,.2f  min=%,.2f  max=%,.2f%n",
                        report.departmentId(), report.headCount(),
                        report.averageSalary(), report.minSalary(), report.maxSalary()));

        printSection("Salary Analytics — top 5 highest salaries");
        analyticsCtrl.top5BySalary().forEach(e ->
                System.out.printf("  %-20s  dept=%-3d  salary=%,.2f%n",
                        e.getName(), e.getDepartmentId(), e.getSalary()));

        printSection("Salary Analytics — average salary per role");
        analyticsCtrl.averageSalaryByRole().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("  %-20s  avg=%,.2f%n", entry.getKey(), entry.getValue()));

        printSection("Salary Analytics — partition active vs inactive");
        Map<Boolean, List<Employee>> partition = analyticsCtrl.partitionByStatus();
        System.out.println("  ACTIVE   (" + partition.get(true).size()  + "):");
        partition.get(true) .forEach(e -> System.out.printf("    → %s%n", e.getName()));
        System.out.println("  INACTIVE (" + partition.get(false).size() + "):");
        partition.get(false).forEach(e -> System.out.printf("    → %s%n", e.getName()));

        printSection("Salary Analytics — group employees by role");
        analyticsCtrl.groupByRole().forEach((role, employees) -> {
            System.out.printf("  [%s] (%d employees)%n", role, employees.size());
            employees.forEach(e -> System.out.printf("      %-20s  salary=%,.2f%n",
                    e.getName(), e.getSalary()));
        });

        printSection("Salary Analytics — full report (all analytics bundled)");
        SalaryAnalyticsReport report = analyticsCtrl.buildReport();
        if (report != null) {
            System.out.println("  Departments covered : " + report.byDepartment().size());
            System.out.println("  Top-5 employees     : " + report.top5BySalary().size());
            System.out.println("  Distinct roles      : " + report.avgSalaryByRole().size());
            System.out.println("  Active employees    : " + report.activeEmployees().size());
            System.out.println("  Inactive employees  : " + report.inactiveEmployees().size());
            System.out.println("  Role groups         : " + report.byRole().size());
        }

        // ════════════════════════════════════════════════════════════════════
        // PART 6 — Design Patterns: what was used and WHY
        //
        // Each sub-section names the pattern, states the problem it solved,
        // and proves it is working by running live code.
        // ════════════════════════════════════════════════════════════════════

        printBanner("PART 6 — Design Patterns: what was used and why");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 1 — Builder
        //
        // PROBLEM: PermanentEmployee and ContractEmployee constructors take
        //          8–9 positional parameters.  Passing the wrong value to the
        //          wrong position causes silent bugs that are hard to spot in
        //          a code review.
        //
        // SOLUTION: Each class exposes a static nested Builder.  Callers set
        //          only the fields they care about, in any order, and the
        //          intent of each assignment is self-documenting.
        //
        // PROOF: Both the readable Builder path and the positional constructor
        //        produce equal objects.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 1 — Builder (PermanentEmployee / ContractEmployee)");

        // ── fluent Builder path ──────────────────────────────────────────────
        PermanentEmployee builtAlice = PermanentEmployee.builder()
                .id(101)
                .name("Alice Kumar")
                .email("alice.builder@example.com")
                .departmentId(10)
                .role("Engineer")
                .salary(85_000)
                .joiningDate(LocalDate.of(2020, 6, 1))
                .gratuityEligible(true)
                .build();

        // ── equivalent positional constructor ───────────────────────────────
        PermanentEmployee ctorAlice = new PermanentEmployee(
                101, "Alice Kumar", "alice.builder@example.com",
                10, "Engineer", 85_000,
                EmployeeStatus.ACTIVE, LocalDate.of(2020, 6, 1), true);

        System.out.println("  Builder result  : " + builtAlice);
        System.out.println("  Ctor result     : " + ctorAlice);
        System.out.println("  Equal?          : " + builtAlice.equals(ctorAlice)
                + "  ← same id → same logical object");
        System.out.println();
        System.out.println("  WHY Builder?");
        System.out.println("    • 9 positional params are easy to misordering silently.");
        System.out.println("    • Builder makes each field name explicit at the call site.");
        System.out.println("    • Default status=ACTIVE is baked in — callers only override");
        System.out.println("      when they truly need a different status.");

        // ContractEmployee Builder with a future end date
        ContractEmployee builtCarol = ContractEmployee.builder()
                .id(102)
                .name("Carol Menon")
                .email("carol.builder@example.com")
                .departmentId(20)
                .role("Designer")
                .salary(60_000)
                .joiningDate(LocalDate.of(2023, 1, 1))
                .contractEndDate(LocalDate.of(2027, 12, 31))
                .build();

        System.out.printf("%n  Contract employee (Builder): %s%n", builtCarol.getName());
        System.out.printf("  Expires: %s  |  isExpired(): %s%n",
                builtCarol.getContractEndDate(), builtCarol.isExpired());

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 2 — Factory Method
        //
        // PROBLEM: Callers who want an Employee must choose PermanentEmployee
        //          vs ContractEmployee, know which builder fields to set, and
        //          keep that knowledge in sync everywhere.  Any change to
        //          construction leaks into all call-sites.
        //
        // SOLUTION: EmployeeFactory provides named static factory methods that
        //          encapsulate the Builder invocation.  Callers depend only on
        //          the Employee abstraction, not the concrete subclass.
        //
        // PROOF: Both employees are created via factory; their concrete types
        //        are exposed only to show the sealed hierarchy is correct.
        // ───────────────────────────────────────────────────────────────���────
        printSection("Pattern 2 — Factory Method (EmployeeFactory)");

        Employee factoryPermanent = EmployeeFactory.createPermanentEmployee(
                201, "Dave Patel", "dave.factory@example.com",
                20, "Manager", 110_000,
                LocalDate.of(2017, 8, 20), true);

        Employee factoryContract = EmployeeFactory.createContractEmployee(
                202, "Lisa Chen", "lisa.factory@example.com",
                20, "Designer", 60_000,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));

        System.out.printf("  Factory → Permanent : %-20s  type=%s%n",
                factoryPermanent.getName(), factoryPermanent.getEmployeeType());
        System.out.printf("  Factory → Contract  : %-20s  type=%s%n",
                factoryContract.getName(),  factoryContract.getEmployeeType());
        System.out.println();
        System.out.println("  WHY Factory Method?");
        System.out.println("    • One call site to change if construction logic evolves.");
        System.out.println("    • Intent-revealing names: createPermanent vs createContract.");
        System.out.println("    • Callers are fully decoupled from PermanentEmployee /");
        System.out.println("      ContractEmployee — they hold an Employee reference.");
        System.out.println("    • Adding a new employee type = one new factory method;");
        System.out.println("      zero existing call-sites change.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 3 — Singleton  (Initialization-on-demand holder)
        //
        // PROBLEM: PayrollServiceImpl needs a strategy registry.  If every
        //          service instance created its own registry and re-registered
        //          both strategies, we'd waste allocations and risk inconsistent
        //          state if different instances had different registrations.
        //
        // SOLUTION: PayrollStrategyRegistry.getInstance() returns the ONE
        //          shared, fully-wired registry.  The holder inner class
        //          guarantees lazy init and thread safety without locks.
        //
        // PROOF: Three calls to getInstance() return the same reference.
        //        A separate new PayrollStrategyRegistry() is a different object
        //        — showing testability is NOT sacrificed.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 3 — Singleton (PayrollStrategyRegistry)");

        PayrollStrategyRegistry s1 = PayrollStrategyRegistry.getInstance();
        PayrollStrategyRegistry s2 = PayrollStrategyRegistry.getInstance();
        PayrollStrategyRegistry s3 = PayrollStrategyRegistry.getInstance();

        System.out.println("  s1 == s2 : " + (s1 == s2) + "  ← same reference");
        System.out.println("  s2 == s3 : " + (s2 == s3) + "  ← same reference");
        System.out.println("  Identity hash s1: " + System.identityHashCode(s1));
        System.out.println("  Identity hash s2: " + System.identityHashCode(s2));

        // Isolated test instance — proves the public constructor is preserved
        PayrollStrategyRegistry isolated = new PayrollStrategyRegistry()
                .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy())
                .register(ContractEmployee.class,  new ContractEmployeePayrollStrategy());

        System.out.println("  isolated == s1   : " + (isolated == s1)
                + "  ← different object; singleton untouched");
        System.out.println();
        System.out.println("  WHY Singleton?");
        System.out.println("    • One shared, fully-wired registry avoids redundant allocations.");
        System.out.println("    • Holder idiom: lazy (created only when first needed),");
        System.out.println("      thread-safe (class loader guarantees atomicity), no locks.");
        System.out.println("    • Public constructor preserved → unit tests create isolated");
        System.out.println("      registries without touching the application singleton.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 4 — Abstract Factory
        //
        // PROBLEM: App.java originally wired every repository, service, and
        //          controller manually with `new`.  The entire object graph was
        //          hardcoded, making it impossible to swap implementations
        //          without editing the entry point.
        //
        // SOLUTION: ApplicationFactory defines an interface for creating a
        //          family of related objects.  InMemoryApplicationFactory wires
        //          the in-memory stack once; a future DatabaseApplicationFactory
        //          could replace it with zero changes to the rest of the code.
        //
        // PROOF: Two separate factory instances each produce controllers backed
        //        by their own independent repositories — no shared state leaks.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 4 — Abstract Factory (ApplicationFactory)");

        ApplicationFactory factoryA = new InMemoryApplicationFactory();
        ApplicationFactory factoryB = new InMemoryApplicationFactory();

        EmployeeController ctrlA = factoryA.createEmployeeController();
        EmployeeController ctrlB = factoryB.createEmployeeController();

        Employee shared = EmployeeFactory.createPermanentEmployee(
                301, "Test User", "test.factory@example.com",
                10, "Engineer", 75_000,
                LocalDate.of(2022, 1, 1), false);

        ctrlA.addEmployee(shared);

        System.out.println("  ctrlA count after add : " + ctrlA.countEmployees()
                + "  ← employee added to A's repo");
        System.out.println("  ctrlB count after add : " + ctrlB.countEmployees()
                + "  ← B's repo is completely independent");
        System.out.println();
        System.out.println("  WHY Abstract Factory?");
        System.out.println("    • The entire controller/service/repository stack is wired");
        System.out.println("      in one place — no manual 'new' chains in App.java.");
        System.out.println("    • ValidationService is wired with the shared repository");
        System.out.println("      (for email uniqueness) and Set.of(10,20,30) for dept checks.");
        System.out.println("    • Swapping to a DB-backed stack = change one line:");
        System.out.println("      ApplicationFactory f = new DatabaseApplicationFactory();");
        System.out.println("    • Each factory instance owns its own object graph —");
        System.out.println("      tests can spin up isolated stacks with no side-effects.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 5 — Strategy
        //
        // PROBLEM: Payroll tax rules differ by employee type (20% for permanent,
        //          10% for contract).  Encoding this with instanceof or if/else
        //          inside PayrollServiceImpl would require editing the service
        //          every time a new employee type is introduced.
        //
        // SOLUTION: PayrollStrategy is an interface.  Each employee type has its
        //          own strategy class.  PayrollServiceImpl asks the registry to
        //          resolve the right strategy at runtime and delegates to it —
        //          the service itself never changes when new types are added.
        //
        // PROOF: Same service call produces different tax amounts depending on
        //        which concrete employee type is passed in.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 5 — Strategy (PayrollStrategy / PayrollStrategyRegistry)");

        Employee stratPermanent = EmployeeFactory.createPermanentEmployee(
                401, "Strat Perm", "strat.perm@example.com",
                10, "Engineer", 100_000,
                LocalDate.of(2020, 1, 1), true);

        Employee stratContract = EmployeeFactory.createContractEmployee(
                402, "Strat Cont", "strat.cont@example.com",
                10, "Engineer", 100_000,
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1));

        // Both employees earn 100_000 — only the strategy differs
        PayrollRecord stratPr1 = payCtrl.processPayroll(401, stratPermanent, payrollMonth);
        PayrollRecord stratPr2 = payCtrl.processPayroll(402, stratContract,  payrollMonth);

        System.out.println("  Same gross salary (100,000) — different strategy applied:");
        if (stratPr1 != null)
            System.out.printf("    PermanentEmployee → tax=%.0f (20%%)  net=%.0f%n",
                    stratPr1.taxAmount(), stratPr1.netSalary());
        if (stratPr2 != null)
            System.out.printf("    ContractEmployee  → tax=%.0f (10%%)  net=%.0f%n",
                    stratPr2.taxAmount(), stratPr2.netSalary());
        System.out.println();
        System.out.println("  WHY Strategy?");
        System.out.println("    • PayrollServiceImpl never uses instanceof or if/else.");
        System.out.println("    • Adding a new employee type = register one new strategy;");
        System.out.println("      PayrollServiceImpl stays unchanged (Open/Closed Principle).");
        System.out.println("    • Strategies are independently testable units.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 6 — Global Exception Handler  (Exception Mapper)
        //
        // PROBLEM: Without a central handler, every controller method needed
        //          its own multi-catch ladder with repeated formatting logic.
        //          Adding a new exception subtype meant updating every catch
        //          block in every controller.
        //
        // SOLUTION: GlobalExceptionHandler.handle(EmployeeException) maps any
        //          subtype to a structured ErrorResponse using instanceof pattern
        //          matching.  Controllers have a single catch(EmployeeException)
        //          and delegate entirely to the handler.
        //
        // PROOF: Show that each exception subtype produces the correct error
        //        code and field name from a single handle() call.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 6 — Global Exception Handler (GlobalExceptionHandler)");

        List<EmployeeException> examplesExceptions = List.of(
                new DuplicateEmployeeException(1),
                new DuplicateEmailException("alice@example.com"),
                new DepartmentNotFoundException(99),
                new EmployeeNotFoundException(7),
                new ValidationException("salary", -500, "must not be negative"),
                new InvalidEmployeeDataException("salaryRange", "-1..50", "min must not be negative"),
                new PayrollException(5, "contract expired")
        );

        System.out.printf("  %-30s  %-25s  %-15s%n", "Exception type", "errorCode", "field");
        System.out.println("  " + "-".repeat(75));
        for (EmployeeException ex : examplesExceptions) {
            GlobalExceptionHandler.ErrorResponse resp = GlobalExceptionHandler.handle(ex);
            System.out.printf("  %-30s  %-25s  %-15s%n",
                    ex.getClass().getSimpleName(),
                    resp.errorCode(),
                    resp.field() != null ? resp.field() : "(none)");
        }
        System.out.println();
        System.out.println("  WHY Global Exception Handler?");
        System.out.println("    • Single place to add/change error-code mappings.");
        System.out.println("    • Controllers shrink to one catch(EmployeeException) each.");
        System.out.println("    • handleAndLog() adds stderr logging without touching the");
        System.out.println("      mapping logic — separation of concerns.");
        System.out.println("    • ErrorResponse is a Record: immutable, auto equals/toString.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 7 — Comparator Chaining  (Behavioural / Composition)
        //
        // PROBLEM: When sorting employees by salary, ties between equal salaries
        //          produce a non-deterministic order that varies across JVM runs
        //          and makes tests fragile.
        //
        // SOLUTION: A three-key chained Comparator (salary ↓ → name ↑ → id ↑)
        //          guarantees a total, stable ordering.  .thenComparing() composes
        //          sort keys without any if/else logic.
        //
        // PROOF: Two employees with identical salaries are always sorted
        //        alphabetically by name, then by id.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 7 — Comparator Chaining (topNBySalary / groupByRole)");

        // Three employees — bob and frank share 90,000
        PermanentEmployee chainBob = new PermanentEmployee(
                501, "Bob Singh",  "chain.bob@example.com",
                10, "Engineer", 90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), true);

        PermanentEmployee chainFrank = new PermanentEmployee(
                502, "Frank Rao", "chain.frank@example.com",
                10, "Engineer", 90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2021, 2, 1), true);

        PermanentEmployee chainDave = new PermanentEmployee(
                503, "Dave Patel", "chain.dave@example.com",
                20, "Manager", 110_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2017, 8, 20), true);

        Comparator<Employee> chain =
                Comparator.comparingDouble(Employee::getSalary).reversed()
                          .thenComparing(Employee::getName)
                          .thenComparingInt(Employee::getId);

        List<Employee> chainInput = List.of(chainFrank, chainBob, chainDave); // intentionally unordered
        List<Employee> sorted = chainInput.stream()
                .sorted(chain)
                .toList();

        System.out.println("  Input order  : " + chainInput.stream()
                .map(e -> e.getName() + "(" + (int)e.getSalary() + ")")
                .toList());
        System.out.println("  Sorted order : " + sorted.stream()
                .map(e -> e.getName() + "(" + (int)e.getSalary() + ")")
                .toList());
        System.out.println();
        System.out.println("  Tiebreaker proof (both have salary=90,000):");
        System.out.printf("    Position 2: %-12s  (B < F alphabetically)%n", sorted.get(1).getName());
        System.out.printf("    Position 3: %-12s%n", sorted.get(2).getName());
        System.out.println();
        System.out.println("  WHY Comparator chaining?");
        System.out.println("    • .thenComparing() composes sort keys declaratively —");
        System.out.println("      no if/else, no null checks, pure function composition.");
        System.out.println("    • Guarantees a total ordering: no two employees are 'equal'");
        System.out.println("      in the sort because ids are unique.");
        System.out.println("    • The same constant is reused in both topNBySalary and");
        System.out.println("      groupByRole — DRY, single source of truth for ordering.");

        // ────────────────────────────────────────────────────────────────────
        // PATTERN 8 — Validation Strategy  (Three-tier validation service)
        //
        // PROBLEM: Validation had three concerns mixed together: field-level
        //          rules, email uniqueness (requires the repository), and
        //          department existence (requires a known-departments set).
        //          Coupling these into one rigid method made unit-testing hard.
        //
        // SOLUTION: EmployeeValidationService accepts optional dependencies.
        //          The no-arg constructor skips uniqueness/dept checks (for
        //          unit tests); the full constructor enables all three tiers.
        //
        // PROOF: Show field violation, email conflict, and unknown department
        //        each produce the correct typed exception.
        // ────────────────────────────────────────────────────────────────────
        printSection("Pattern 8 — Three-Tier Validation (EmployeeValidationService)");

        // Full validator: repo with alice pre-loaded, depts 10/20/30 known
        var validationRepo = new com.example.helloworld.repository.inmemory.InMemoryEmployeeRepository();
        validationRepo.add(alice);   // alice owns "alice@example.com"

        EmployeeValidationService fullValidator =
                new EmployeeValidationService(validationRepo, Set.of(10, 20, 30));

        // ── Tier 1: field rule — email missing '@'
        Employee badEmail = new PermanentEmployee(
                601, "Bad Email", "noemail", 10, "Eng",
                50_000, EmployeeStatus.ACTIVE, LocalDate.of(2022, 1, 1), false);
        try {
            fullValidator.validate(badEmail);
        } catch (ValidationException e) {
            System.out.printf("  Tier 1 (field rule)        → %s: %s%n",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        // ── Tier 2: email uniqueness — alice's email re-used by a different id
        Employee dupEmail = new PermanentEmployee(
                602, "Alice Clone", "alice@example.com", 10, "Eng",
                50_000, EmployeeStatus.ACTIVE, LocalDate.of(2022, 1, 1), false);
        try {
            fullValidator.validate(dupEmail);
        } catch (DuplicateEmailException e) {
            System.out.printf("  Tier 2 (email uniqueness)  → %s: %s%n",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        // ── Tier 3: department existence — dept 99 not in Set.of(10,20,30)
        Employee unknownDept = new PermanentEmployee(
                603, "Unknown Dept", "unknowndept@example.com", 99, "Eng",
                50_000, EmployeeStatus.ACTIVE, LocalDate.of(2022, 1, 1), false);
        try {
            fullValidator.validate(unknownDept);
        } catch (DepartmentNotFoundException e) {
            System.out.printf("  Tier 3 (dept existence)    → %s: %s%n",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        System.out.println();
        System.out.println("  WHY three-tier validation?");
        System.out.println("    • Field rules need no external dependencies → fast, pure.");
        System.out.println("    • Email uniqueness requires the repository → injected.");
        System.out.println("    • Dept existence requires a known-departments set → injected.");
        System.out.println("    • No-arg constructor skips tiers 2+3: unit tests remain");
        System.out.println("      simple with no mocking required.");
        System.out.println("    • Each tier throws a different typed exception so callers");
        System.out.println("      can catch exactly what they care about.");

        // ────────────────────────────────────────────────────────────────────
        // SUMMARY TABLE
        // ────────────────────────────────────────────────────────────────────
        printSection("Design Pattern Summary");
        System.out.printf("  %-28s  %-38s  %s%n", "Pattern", "Applied To", "Core Benefit");
        System.out.println("  " + "-".repeat(100));
        Object[][] summary = {
            {"Builder",                  "PermanentEmployee / ContractEmployee",  "Readable, safe multi-param construction"   },
            {"Factory Method",           "EmployeeFactory",                        "Named creation; callers see only Employee"  },
            {"Singleton (IoD holder)",   "PayrollStrategyRegistry",               "One shared registry; lazy + thread-safe"    },
            {"Abstract Factory",         "ApplicationFactory",                     "Full stack wired in one place; swap-ready"  },
            {"Strategy",                 "PayrollStrategy + Registry",             "Open/Closed: new type = new strategy only"  },
            {"Global Exception Handler", "GlobalExceptionHandler",                 "Central mapping; one catch per controller"  },
            {"Comparator Chaining",      "SalaryAnalyticsServiceImpl",             "Total order; no ties; declarative, DRY"     },
            {"Three-Tier Validation",    "EmployeeValidationService",              "Layered rules; testable at each tier"       },
        };
        for (Object[] row : summary) {
            System.out.printf("  %-28s  %-38s  %s%n", row[0], row[1], row[2]);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void printSection(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 55 - title.length())));
    }

    private static void printBanner(String title) {
        String border = "═".repeat(title.length() + 4);
        System.out.println("\n" + border);
        System.out.println("  " + title);
        System.out.println(border);
    }
}
