# Java Re-Skill Program — Layered Design with OOP, Creational Patterns & Stream Analytics

## Overview

This project demonstrates a **layered Java application** applying core OOP concepts in Java 17,
including **sealed class hierarchies**, **records**, **stream pipelines**, **encapsulation**,
**abstraction**, and **inheritance**, organised into five distinct layers following the
**Controller → Service → Repository** architecture.

It applies all five **Creational Design Patterns** (GoF) wherever object construction
complexity justifies them, and adds a dedicated **Salary Analytics** sub-system built
entirely on Java Stream pipelines (no loops).

| Pattern              | Applied To                                          | Benefit                                                       |
|----------------------|-----------------------------------------------------|---------------------------------------------------------------|
| **Builder**          | `PermanentEmployee`, `ContractEmployee`             | Readable, step-by-step construction of complex domain objects |
| **Factory Method**   | `EmployeeFactory`                                   | Centralised, named creation hiding concrete subclass details  |
| **Singleton**        | `PayrollStrategyRegistry`                           | One shared, fully-wired strategy registry across the app      |
| **Abstract Factory** | `ApplicationFactory` / `InMemoryApplicationFactory` | Wires the entire controller/service/repo stack in one place   |

---

## Architecture

```
App
 │
 ├── [Abstract Factory] InMemoryApplicationFactory
 │        │
 │        ├── creates EmployeeController       ──►  EmployeeService        ──►  EmployeeRepository
 │        │           │                                                           └── InMemoryEmployeeRepository
 │        │           └── (validates via) ValidationService
 │        │
 │        ├── creates PayrollController        ──►  PayrollService         ──►  PayrollStrategyResolver
 │        │                                                                        └── [Singleton] PayrollStrategyRegistry
 │        │                                                                              ├── PermanentEmployeePayrollStrategy
 │        │                                                                              └── ContractEmployeePayrollStrategy
 │        │
 │        └── creates SalaryAnalyticsController ──► SalaryAnalyticsService (stream pipelines)
 │                    │                               ├── groupByDepartment   → DepartmentSalaryReport per dept
 │                    │                               ├── topNBySalary        → top-N employees by salary
 │                    │                               ├── averageSalaryByRole → avg salary keyed by role
 │                    │                               ├── partitionByStatus   → ACTIVE / INACTIVE buckets
 │                    └── (fetches employees via) EmployeeService
 │
 └── [Factory Method] EmployeeFactory
          ├── createPermanentEmployee(...)  ──►  [Builder] PermanentEmployee.builder().build()
          └── createContractEmployee(...)   ──►  [Builder] ContractEmployee.builder().build()
```

The **Controller layer** is the only entry point for callers. It:
- Validates input using `ValidationService` before mutating state.
- Delegates business operations to the appropriate `Service`.
- Catches **all** checked exceptions — no exceptions leak to the caller.

---

## Project Structure

```
src/main/java/com/example/helloworld/
├── App.java                                        — Entry point / demo runner (Parts 1–5)
├── factory/                                        — ★ Abstract Factory
│   ├── ApplicationFactory.java                     — Abstract factory interface
│   └── InMemoryApplicationFactory.java             — Concrete factory: in-memory stack
├── controller/
│   ├── EmployeeController.java                     — Employee requests; catches all exceptions
│   ├── PayrollController.java                      — Payroll requests; catches all exceptions
│   └── SalaryAnalyticsController.java              — Analytics requests; catches all exceptions
├── domain/
│   ├── EmployeeStatus.java                         — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                               — Sealed abstract base class
│   ├── PermanentEmployee.java                      — Final subclass + ★ Builder
│   ├── ContractEmployee.java                       — Final subclass + ★ Builder
│   ├── EmployeeFactory.java                        — ★ Factory Method: named employee creation
│   ├── Department.java                             — Record (immutable DTO)
│   ├── PayrollRecord.java                          — Record (immutable DTO)
│   ├── DepartmentSalaryReport.java                 — Record: per-dept salary aggregates
│   ├── SalaryAnalyticsReport.java                  — Record: full analytics bundle
│   ├── EmployeeSummaryDTO.java                     — Record: read-only employee projection
│   └── payroll/
│       ├── PayrollStrategy.java                    — Interface: payroll calculation strategy
│       ├── PermanentEmployeePayrollStrategy.java   — Impl: 20% tax for permanent employees
│       └── ContractEmployeePayrollStrategy.java    — Impl: 10% tax, rejects expired contracts
├── repository/
│   ├── EmployeeRepository.java                     — Interface: repository contract
│   ├── EmployeeKey.java                            — Custom HashMap key (id + email)
│   ├── DepartmentKey.java                          — Record-based HashMap key
│   └── inmemory/
│       └── InMemoryEmployeeRepository.java         — Collections-backed implementation
├── service/
│   ├── EmployeeService.java                        — Interface: employee operations
│   ├── EmployeeServiceImpl.java                    — Delegates to EmployeeRepository
│   ├── ValidationService.java                      — Interface: employee validation
│   ├── EmployeeValidationService.java              — Impl: business rule validation
│   ├── PayrollService.java                         — Interface: payroll processing
│   ├── PayrollServiceImpl.java                     — Payroll orchestration (uses a resolver)
│   ├── PayrollStrategyResolver.java                — Interface: resolves strategy for an employee
│   ├── PayrollStrategyRegistry.java                — ★ Singleton registry/lookup
│   ├── SalaryAnalyticsService.java                 — Interface: stream-based salary analytics
│   └── SalaryAnalyticsServiceImpl.java             — Impl: all aggregates via stream pipelines
└── exception/
    ├── EmployeeException.java                      — Base checked exception
    ├── DuplicateEmployeeException.java
    ├── DuplicateEmailException.java
    ├── EmployeeNotFoundException.java
    ├── InvalidEmployeeDataException.java
    ├── PayrollException.java
    └── ValidationException.java

src/test/java/com/example/helloworld/
├── controller/
│   ├── EmployeeControllerTest.java                 — Mockito tests for EmployeeController
│   └── PayrollControllerTest.java                  — Mockito tests for PayrollController
├── domain/
│   ├── DepartmentSalaryReportTest.java             — Record aggregates + compact constructor
│   ├── EmployeeSummaryDTOTest.java                 — DTO projection + switch expression
│   └── payroll/
│       └── PayrollStrategyTest.java                — Strategy tax calculations
└── service/
    ├── EmployeeServiceImplTest.java                — Mockito: delegation + Optional + parametrized
    ├── EmployeeValidationServiceTest.java          — Validation rules + parametrized inputs
    ├── PayrollServiceImplTest.java                 — Tax calc, batch, null guards, parametrized
    ├── PayrollStrategyRegistryTest.java            — Singleton registry resolution
    └── SalaryAnalyticsServiceImplTest.java         — 55 tests: parametrized, edge cases, Optional
```

---

## Salary Analytics (Part 5)

A dedicated analytics sub-system built entirely on **Java Stream pipelines** — no manual
loops anywhere in the implementation.

### Flow

```
SalaryAnalyticsController
    │
    ├── fetches all employees via  EmployeeService.getAllEmployees()
    └── delegates computation to   SalaryAnalyticsService
                                       │
                                       ├── groupByDepartment   → Collectors.groupingBy + summaryStatistics
                                       ├── topNBySalary        → sorted().limit()
                                       ├── averageSalaryByRole → groupingBy + averagingDouble
                                       └── partitionByStatus   → Collectors.partitioningBy
```

### `SalaryAnalyticsService` — Interface

All methods receive the employee list as a parameter — the service is **stateless** and
has no repository dependency, making it trivially testable.

| Method                                       | Returns                              | Stream operation used                            |
|----------------------------------------------|--------------------------------------|--------------------------------------------------|
| `groupByDepartment(employees)`               | `Map<Integer, DepartmentSalaryReport>` | `groupingBy` + `summaryStatistics`               |
| `topNBySalary(employees, n)`                 | `List<Employee>`                     | `sorted(reversed()).limit(n)`                    |
| `averageSalaryByRole(employees)`             | `Map<String, Double>`                | `groupingBy(role) + averagingDouble(salary)`     |
| `partitionByStatus(employees)`               | `Map<Boolean, List<Employee>>`       | `partitioningBy(status == ACTIVE)`               |
| `buildReport(employees)`                     | `SalaryAnalyticsReport`              | Calls all four above; bundles into one record    |

### `SalaryAnalyticsController` — Methods

| Method                  | Returns                              | On error                  |
|-------------------------|--------------------------------------|---------------------------|
| `groupByDepartment()`   | `Map<Integer, DepartmentSalaryReport>` | empty map, logs to stderr |
| `top5BySalary()`        | `List<Employee>`                     | empty list, logs to stderr |
| `averageSalaryByRole()` | `Map<String, Double>`                | empty map, logs to stderr |
| `partitionByStatus()`   | `Map<Boolean, List<Employee>>`       | both buckets empty        |
| `buildReport()`         | `SalaryAnalyticsReport`              | `null`, logs to stderr    |

### `DepartmentSalaryReport` (Record)

Immutable per-department salary summary. Built via `DepartmentSalaryReport.of(deptId, employees)`
which derives all aggregates from a single stream pass.

| Field           | Type     | Description                    |
|-----------------|----------|--------------------------------|
| `departmentId`  | `int`    | Department being reported on   |
| `headCount`     | `int`    | Number of employees            |
| `totalSalary`   | `double` | Sum of all salaries            |
| `averageSalary` | `double` | Mean salary                    |
| `minSalary`     | `double` | Lowest salary in the group     |
| `maxSalary`     | `double` | Highest salary in the group    |

### `SalaryAnalyticsReport` (Record)

Bundles all four analytics views into a single immutable snapshot.

| Field               | Type                                   | Description                              |
|---------------------|----------------------------------------|------------------------------------------|
| `byDepartment`      | `Map<Integer, DepartmentSalaryReport>` | Per-department salary summary            |
| `top5BySalary`      | `List<Employee>`                       | Up to 5 highest earners, desc order      |
| `avgSalaryByRole`   | `Map<String, Double>`                  | Average salary keyed by lower-cased role |
| `activeEmployees`   | `List<Employee>`                       | Employees with `ACTIVE` status           |
| `inactiveEmployees` | `List<Employee>`                       | Employees with `INACTIVE` status         |

### Stream Pipeline Design Rules

1. **No loops** — every aggregation uses a stream collector or intermediate operation.
2. **Unmodifiable results** — all returned collections are wrapped via `toUnmodifiableList()`,
   `toUnmodifiableMap()`, or `Collections.unmodifiableMap()`.
3. **Stateless** — `SalaryAnalyticsServiceImpl` has no fields; every method is a pure function.
4. **Key normalisation** — role keys are `.strip().toLowerCase()` so `"Engineer"`,
   `"ENGINEER"`, and `"  engineer  "` all collapse to the same bucket.

---

## Creational Design Patterns

### ★ Builder — `PermanentEmployee` & `ContractEmployee`

**Problem:** Both employee classes have long constructors (8–9 parameters). Passing positional
arguments is error-prone and unreadable.

**Solution:** Each class exposes a static nested `Builder` that lets callers set only what
they need, in any order, with a fluent API.

```java
// Before — positional constructor, hard to read
Employee alice = new PermanentEmployee(
        1, "Alice Kumar", "alice@example.com", 10, "Engineer",
        85_000, EmployeeStatus.ACTIVE, LocalDate.of(2020, 6, 1), true);

// After — Builder: self-documenting, resilient to parameter reordering
Employee alice = PermanentEmployee.builder()
        .id(1).name("Alice Kumar").email("alice@example.com")
        .departmentId(10).role("Engineer").salary(85_000)
        .joiningDate(LocalDate.of(2020, 6, 1))
        .gratuityEligible(true)
        .build();

// ContractEmployee Builder
Employee carol = ContractEmployee.builder()
        .id(3).name("Carol Menon").email("carol@example.com")
        .departmentId(20).role("Designer").salary(60_000)
        .joiningDate(LocalDate.of(2023, 1, 1))
        .contractEndDate(LocalDate.of(2025, 12, 31))
        .build();
```

> The default `status` is `EmployeeStatus.ACTIVE`. Call `.status(EmployeeStatus.INACTIVE)`
> on the builder when you need to override it.

---

### ★ Factory Method — `EmployeeFactory`

**Problem:** Callers need to know which concrete subclass to instantiate and which builder
fields to set — coupling them to implementation details.

**Solution:** `EmployeeFactory` provides **named static factory methods** that encapsulate
the builder calls behind an intent-revealing API. Callers depend only on `Employee`.

```java
Employee alice = EmployeeFactory.createPermanentEmployee(
        1, "Alice Kumar", "alice@example.com",
        10, "Engineer", 85_000,
        LocalDate.of(2020, 6, 1), true);

Employee carol = EmployeeFactory.createContractEmployee(
        3, "Carol Menon", "carol@example.com",
        20, "Designer", 60_000,
        LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));
```

**Adding a new employee type** only requires a new factory method in `EmployeeFactory` — no
existing call-sites change.

---

### ★ Singleton — `PayrollStrategyRegistry`

**Problem:** `PayrollServiceImpl` created a brand-new registry and registered both strategies
on every instantiation, wasting allocations and producing inconsistent state.

**Solution:** Uses the **Initialization-on-demand holder** idiom — lazy, thread-safe, no
synchronisation overhead.

```java
// One shared, fully-wired instance — lazy, thread-safe
PayrollStrategyRegistry registry = PayrollStrategyRegistry.getInstance();
```

**Testability is preserved** — the public constructor is kept so unit tests create
isolated registries without touching the singleton:

```java
PayrollStrategyRegistry registry = new PayrollStrategyRegistry()
        .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy());
```

**Extending payroll for a new employee type:**

```java
PayrollStrategyRegistry.getInstance()
        .register(MyNewEmployeeType.class, new MyNewPayrollStrategy());
```

---

### ★ Abstract Factory — `ApplicationFactory` / `InMemoryApplicationFactory`

**Problem:** `App.java` manually instantiated every repository, service, and controller with
`new` — knowledge of the entire object graph was hardcoded in the entry point.

**Solution:** `ApplicationFactory` defines an interface for creating a **family of related
objects**. `InMemoryApplicationFactory` wires the in-memory stack and caches instances so all
controllers share the same repository.

```java
ApplicationFactory factory = new InMemoryApplicationFactory();

EmployeeController        empCtrl      = factory.createEmployeeController();
PayrollController         payCtrl      = factory.createPayrollController();
SalaryAnalyticsController analyticsCtrl = factory.createSalaryAnalyticsController();
```

**Swapping the entire stack** requires changing only the factory:

```java
ApplicationFactory factory = new DatabaseApplicationFactory(); // drop-in
```

| Method                               | Returns                      | Notes                                   |
|--------------------------------------|------------------------------|-----------------------------------------|
| `createEmployeeRepository()`         | `EmployeeRepository`         | Cached — same instance per factory      |
| `createEmployeeService()`            | `EmployeeService`            | Cached — wired to the shared repository |
| `createValidationService()`          | `ValidationService`          | Cached                                  |
| `createPayrollService()`             | `PayrollService`             | Cached — uses singleton registry        |
| `createSalaryAnalyticsService()`     | `SalaryAnalyticsService`     | Cached — stateless service              |
| `createEmployeeController()`         | `EmployeeController`         | New instance each call                  |
| `createPayrollController()`          | `PayrollController`          | New instance each call                  |
| `createSalaryAnalyticsController()`  | `SalaryAnalyticsController`  | New instance each call                  |

---

## Controller Layer

### `EmployeeController`

| Method                             | Behaviour                                                                                                       |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `addEmployee(Employee)`            | Validates → adds; handles `ValidationException`, `DuplicateEmployeeException`, `DuplicateEmailException`       |
| `updateEmployee(Employee)`         | Validates → updates; handles `ValidationException`, `EmployeeNotFoundException`, `DuplicateEmailException`     |
| `removeEmployee(int id)`           | Removes; handles `EmployeeNotFoundException`                                                                    |
| `getById(int id)`                  | Returns `Optional<Employee>`                                                                                    |
| `getByEmail(String)`               | Returns `Optional<Employee>`                                                                                    |
| `getAllEmployees()`                 | Returns all employees                                                                                           |
| `getByDepartment(int)`             | Returns employees in given department                                                                           |
| `getByStatus(EmployeeStatus)`      | Returns employees matching status                                                                               |
| `getByRole(String)`                | Case-insensitive partial match on role                                                                          |
| `getBySalaryRange(double, double)` | Returns employees in range; returns empty list on `InvalidEmployeeDataException`                                |
| `countEmployees()`                 | Total number of employees                                                                                       |
| `totalSalary()`                    | Sum of all salaries                                                                                             |
| `averageSalary()`                  | Average salary                                                                                                  |

### `PayrollController`

| Method                                      | Behaviour                                                                  |
|---------------------------------------------|----------------------------------------------------------------------------|
| `processPayroll(recordId, employee, month)` | Processes one employee; returns `null` on `PayrollException`               |
| `processAll(employees, month)`              | Fault-tolerant batch; returns only successfully processed records          |

### `SalaryAnalyticsController`

See [Salary Analytics](#salary-analytics-part-5) section above.

---

## Domain Layer

### `Employee` (Sealed Abstract Class)

| Field          | Type             | Description                        |
|----------------|------------------|------------------------------------|
| `id`           | `int`            | Unique employee identifier         |
| `name`         | `String`         | Full name                          |
| `email`        | `String`         | Email address                      |
| `departmentId` | `int`            | Reference to Department            |
| `role`         | `String`         | Job role / title                   |
| `salary`       | `double`         | Current salary (mutable)           |
| `status`       | `EmployeeStatus` | `ACTIVE` or `INACTIVE` (mutable)   |
| `joiningDate`  | `LocalDate`      | Date the employee joined           |

- **Sealed hierarchy** — only `PermanentEmployee` and `ContractEmployee` are permitted.
- **`equals()` / `hashCode()`** — identity based on `id`.
- **`getEmployeeType()`** — abstract; forces each subclass to identify itself.

### `PermanentEmployee` *(extends Employee)*

| Additional Field   | Type      | Description                                  |
|--------------------|-----------|----------------------------------------------|
| `gratuityEligible` | `boolean` | Whether the employee qualifies for gratuity  |

### `ContractEmployee` *(extends Employee)*

| Additional Field  | Type        | Description                              |
|-------------------|-------------|------------------------------------------|
| `contractEndDate` | `LocalDate` | Date the contract expires (mutable)      |

- `isExpired()` — returns `true` if the contract has passed today's date.

### `EmployeeSummaryDTO` (Record)

Read-only projection of an `Employee` for display/API responses. Built via
`EmployeeSummaryDTO.from(employee)` which uses a **switch expression** over the sealed
hierarchy to derive `employeeType` and `extraInfo` — exhaustive, no default needed.

| Field          | Type     | Description                                            |
|----------------|----------|--------------------------------------------------------|
| `id`           | `int`    | Employee id                                            |
| `name`         | `String` | Full name                                              |
| `role`         | `String` | Job role                                               |
| `departmentId` | `int`    | Department                                             |
| `salary`       | `double` | Current salary                                         |
| `status`       | `String` | `"ACTIVE"` or `"INACTIVE"`                            |
| `employeeType` | `String` | `"PERMANENT"` or `"CONTRACT"`                         |
| `extraInfo`    | `String` | `"gratuityEligible=true"` or `"contractEnds=<date>"`  |

### Other Domain Records

| Record                  | Purpose                                               |
|-------------------------|-------------------------------------------------------|
| `Department`            | Immutable department DTO (id, name, location)         |
| `PayrollRecord`         | Immutable payroll result (gross, tax, net, timestamp) |
| `DepartmentSalaryReport`| Per-dept salary aggregates — see analytics section    |
| `SalaryAnalyticsReport` | Full analytics bundle — see analytics section         |

### Payroll Strategy (`domain/payroll/`)

| Strategy                              | Tax rate | Guard                          |
|---------------------------------------|----------|--------------------------------|
| `PermanentEmployeePayrollStrategy`    | 20%      | Rejects non-`PermanentEmployee`|
| `ContractEmployeePayrollStrategy`     | 10%      | Rejects expired contracts      |

---

## Repository Layer

### `InMemoryEmployeeRepository`

Collections-backed implementation with **secondary indexes** for O(1) lookups.

| Index             | Type                                   | Purpose                                   |
|-------------------|----------------------------------------|-------------------------------------------|
| `store`           | `HashMap<EmployeeKey, Employee>`       | Primary store                             |
| `idIndex`         | `HashMap<Integer, EmployeeKey>`        | id → key reverse index                    |
| `emailIndex`      | `HashMap<String, Integer>`             | email → id (O(1) uniqueness checks)       |
| `departmentIndex` | `HashMap<DepartmentKey, Set<Integer>>` | dept → Set of ids (O(1) dept queries)     |

### `EmployeeKey` (Custom HashMap Key)

Hand-written `equals()` + `hashCode()` over `id` + `email`. Both fields are `final`.

### `DepartmentKey` (Record-based HashMap Key)

A Java Record — `equals()`, `hashCode()`, and immutability auto-generated by the compiler.

---

## Service Layer

### `EmployeeService` / `EmployeeServiceImpl`

Thin delegation layer between controller and repository. Every method maps 1-to-1 to a
repository call, providing an abstraction boundary so the controller never sees
repository types.

### `ValidationService` / `EmployeeValidationService`

Called by `EmployeeController` before every mutating operation. Throws `ValidationException`
on the first rule violation.

| Rule                                    | Field rejected          |
|-----------------------------------------|-------------------------|
| `email` must contain `@`               | `email`                 |
| `status` must not be `INACTIVE`        | `status`                |
| `ContractEmployee` must not be expired | `contractEndDate`       |

### `PayrollService` / `PayrollServiceImpl`

Orchestrates payroll using the Singleton `PayrollStrategyRegistry`. The no-arg constructor
uses `getInstance()`; the injected constructor is kept for testing.

### `SalaryAnalyticsService` / `SalaryAnalyticsServiceImpl`

All aggregates are computed via **stream pipelines** — no loops.

```java
// groupByDepartment — Collectors.groupingBy + summaryStatistics
employees.stream()
    .collect(groupingBy(Employee::getDepartmentId))
    .entrySet().stream()
    .collect(toUnmodifiableMap(Entry::getKey,
             e -> DepartmentSalaryReport.of(e.getKey(), e.getValue())));

// topNBySalary — sorted + limit
employees.stream()
    .sorted(comparingDouble(Employee::getSalary).reversed())
    .limit(n)
    .collect(toUnmodifiableList());

// averageSalaryByRole — groupingBy + averagingDouble
employees.stream()
    .collect(groupingBy(e -> e.getRole().strip().toLowerCase(),
             averagingDouble(Employee::getSalary)));

// partitionByStatus — partitioningBy (two-bucket specialised collector)
employees.stream()
    .collect(partitioningBy(e -> e.getStatus() == ACTIVE,
             toUnmodifiableList()));
```

---

## Exception Layer

```
EmployeeException  (base checked)
    ├── DuplicateEmployeeException   — id already exists on add
    ├── DuplicateEmailException      — email already taken on add/update
    ├── EmployeeNotFoundException    — id/email not found on update/remove/find
    ├── InvalidEmployeeDataException — field value fails a repository-level rule
    ├── PayrollException             — payroll calculation failure
    └── ValidationException          — employee fails a business rule
```

All exceptions are caught by the **Controller layer** — they never propagate to `App.java`.

---

## Testing

**193 tests — 0 failures.** Three test patterns applied consistently across all test classes:

### 1. Parameterized Tests (`@ParameterizedTest`)

| Test class                       | Parameterized coverage                                                   |
|----------------------------------|--------------------------------------------------------------------------|
| `SalaryAnalyticsServiceImplTest` | `@CsvSource` for dept aggregates, top-N size, rank order, avg by role, partition counts |
| `PayrollServiceImplTest`         | `@CsvSource` for gross/tax/net across salary values for both employee types |
| `EmployeeServiceImplTest`        | `@EnumSource` for all statuses, `@ValueSource` for role strings, `@CsvSource` for salary ranges |
| `EmployeeValidationServiceTest`  | `@ValueSource` for invalid emails and valid salary boundaries, `@CsvSource` for exception fields |
| `DepartmentSalaryReportTest`     | `@CsvSource` for min/avg/max across salary pairs                         |

### 2. Stream Edge-Case Tests

| Edge case                                      | Test class                       |
|------------------------------------------------|----------------------------------|
| All employees have equal salary (tied sort)    | `SalaryAnalyticsServiceImplTest` |
| `n=1`, `n=0`, negative `n` for `topNBySalary` | `SalaryAnalyticsServiceImplTest` |
| Whitespace role names collapse to same key     | `SalaryAnalyticsServiceImplTest` |
| Single-employee dept: min == max == avg        | `SalaryAnalyticsServiceImplTest` |
| All-INACTIVE input: active bucket is empty     | `SalaryAnalyticsServiceImplTest` |
| Single-employee report: every count is 0 or 1 | `SalaryAnalyticsServiceImplTest` |
| 8-employee list: top5 capped at 5              | `SalaryAnalyticsServiceImplTest` |
| Result lists are unmodifiable                  | `SalaryAnalyticsServiceImplTest` |
| Empty input throughout all methods             | `SalaryAnalyticsServiceImplTest` |

### 3. `Optional` Usage Validation

`Optional.ofNullable(map.get(key))` is used in tests instead of raw `.get()` so that:
- **Present values** are asserted with `.isPresent()` / `.orElseThrow()`
- **Absent values** are asserted with `.isEmpty()`
- Failure messages name the missing key explicitly

```java
// Present — fail with a meaningful message if key is absent
DepartmentSalaryReport dept10 =
        Optional.ofNullable(service.groupByDepartment(all).get(10))
                .orElseThrow(() -> new AssertionError("dept 10 missing"));

// Absent — assert the key truly is not in the map
Optional<Double> ceo = Optional.ofNullable(service.averageSalaryByRole(all).get("ceo"));
assertTrue(ceo.isEmpty(), "'ceo' role must not be present");
```

Applied in: `SalaryAnalyticsServiceImplTest` (dept lookups, role lookups, report
field lookups), `EmployeeServiceImplTest` (`getById`, `getByEmail` Optional chain assertions).

---

## OOP Concepts Applied

### 🏗️ Creational Design Patterns
See the dedicated [Creational Design Patterns](#creational-design-patterns) section above.

### 🏛️ Layered Architecture (Controller → Service → Repository)
- **Controller** — input, validation, exception boundaries
- **Service** — business logic, orchestration
- **Repository** — data persistence, indexing

### 🔒 Sealed Class Hierarchy
```
Employee (sealed abstract)
    ├── PermanentEmployee (final)
    └── ContractEmployee  (final)
```
The `sealed` + `permits` keywords restrict the hierarchy to exactly these two subtypes,
enabling **exhaustive switch expressions** without a `default` case.

### 📦 Encapsulation
All fields in `Employee` are `private`. Only mutable fields (`salary`, `status`,
`contractEndDate`) expose setters, each with validation guards.

### 🎭 Abstraction
Every interface (`EmployeeService`, `EmployeeRepository`, `PayrollStrategy`,
`PayrollStrategyResolver`, `ValidationService`, `SalaryAnalyticsService`,
`ApplicationFactory`) defines *what* a layer can do, not *how*.

### 🧬 Inheritance
`PermanentEmployee` and `ContractEmployee` reuse all common fields and constructor
validation from `Employee` via `super(...)`, extending it with their own fields.

### 🎯 Strategy Pattern
`PayrollStrategy` is selected at runtime. New employee types can be supported by
registering a new strategy — `PayrollServiceImpl` never changes.

### 📋 Records (DTOs & Keys)
`Department`, `PayrollRecord`, `DepartmentSalaryReport`, `SalaryAnalyticsReport`,
`EmployeeSummaryDTO`, and `DepartmentKey` are **Java Records** — immutable by design,
with auto-generated `equals()`, `hashCode()`, `toString()`, and accessors.

### 🟰 `equals()` and `hashCode()`

| Class                | Strategy                                       |
|----------------------|------------------------------------------------|
| `Employee`           | Identity by `id`                               |
| `PermanentEmployee`  | Parent equality + `gratuityEligible`           |
| `ContractEmployee`   | Parent equality + `contractEndDate`            |
| `Department`         | Auto-generated by Record (all fields)          |
| `PayrollRecord`      | Auto-generated by Record (all fields)          |
| `DepartmentSalaryReport` | Auto-generated by Record (all fields)      |
| `SalaryAnalyticsReport`  | Auto-generated by Record (all fields)      |
| `EmployeeKey`        | Hand-written — `id` + `email`                  |
| `DepartmentKey`      | Auto-generated by Record — `id` only           |

---

## Build & Run

```bash
# Compile (preview features enabled for sealed switch expressions)
mvn compile

# Run all 193 tests
mvn test

# Run the App demo (Parts 1–5)
mvn exec:java
```

> Requires **Java 17** and **Maven 3.x**.
> `--enable-preview` is configured in `pom.xml` for all three plugins
> (`maven-compiler-plugin`, `maven-surefire-plugin`, `exec-maven-plugin`)
> to support pattern matching in `switch` expressions over sealed hierarchies.
