# Java Re-Skill Program — Layered Design with OOP, Creational Patterns & Stream Analytics

## Overview

This project demonstrates a **layered Java application** applying core OOP concepts in Java 17,
including **sealed class hierarchies**, **records**, **stream pipelines**, **encapsulation**,
**abstraction**, and **inheritance**, organised into five distinct layers following the
**Controller → Service → Repository** architecture.

It applies all five **Creational Design Patterns** (GoF) wherever object construction
complexity justifies them, and adds a dedicated **Salary Analytics** sub-system built
entirely on Java Stream pipelines (no loops), plus a **Global Exception Handler** that
centralises all error mapping across the exception hierarchy.

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
 │        │           └── (validates via) ValidationService ──► EmployeeValidationService
 │        │                                                          ├── field rules (email, salary, status…)
 │        │                                                          ├── unique-email check  (→ repo)
 │        │                                                          └── department-exists check (→ Set<Integer>)
 │        │
 │        ├── creates PayrollController        ──►  PayrollService         ──►  PayrollStrategyResolver
 │        │                                                                        └── [Singleton] PayrollStrategyRegistry
 │        │                                                                              ├── PermanentEmployeePayrollStrategy
 │        │                                                                              └── ContractEmployeePayrollStrategy
 │        │
 │        └── creates SalaryAnalyticsController ──► SalaryAnalyticsService (stream pipelines)
 │                    │                               ├── groupByDepartment   → DepartmentSalaryReport per dept
 │                    │                               ├── topNBySalary        → Comparator chaining (salary ↓, name ↑, id ↑)
 │                    │                               ├── averageSalaryByRole → avg salary keyed by role
 │                    │                               ├── partitionByStatus   → ACTIVE / INACTIVE buckets
 │                    │                               └── groupByRole         → employees grouped by role, sorted within bucket
 │                    └── (fetches employees via) EmployeeService
 │
 ├── [Factory Method] EmployeeFactory
 │        ├── createPermanentEmployee(...)  ──►  [Builder] PermanentEmployee.builder().build()
 │        └── createContractEmployee(...)   ──►  [Builder] ContractEmployee.builder().build()
 │
 └── [Global Exception Handler] GlobalExceptionHandler
          └── handle(EmployeeException) → ErrorResponse { errorCode, message, field }
               ├── DuplicateEmployeeException   → DUPLICATE_EMPLOYEE_ID
               ├── DuplicateEmailException      → DUPLICATE_EMAIL
               ├── DepartmentNotFoundException  → DEPARTMENT_NOT_FOUND
               ├── EmployeeNotFoundException    → EMPLOYEE_NOT_FOUND
               ├── ValidationException          → VALIDATION_ERROR
               ├── InvalidEmployeeDataException → INVALID_DATA
               └── PayrollException             → PAYROLL_ERROR
```

The **Controller layer** is the only entry point for callers. It:
- Validates input using `ValidationService` before mutating state.
- Delegates business operations to the appropriate `Service`.
- Catches **all** checked exceptions via `GlobalExceptionHandler` — no exceptions leak to the caller.

---

## Project Structure

```
src/main/java/com/example/helloworld/
├── App.java                                        — Entry point / demo runner (Parts 1–5)
├── factory/                                        — ★ Abstract Factory
│   ├── ApplicationFactory.java                     — Abstract factory interface
│   └── InMemoryApplicationFactory.java             — Concrete factory: wires repo + validator + dept IDs
├── controller/
│   ├── EmployeeController.java                     — Employee requests; delegates to GlobalExceptionHandler
│   ├── PayrollController.java                      — Payroll requests; catches all exceptions
│   └── SalaryAnalyticsController.java              — Analytics requests; includes groupByRole()
├── domain/
│   ├── EmployeeStatus.java                         — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                               — Sealed abstract base class
│   ├── PermanentEmployee.java                      — Final subclass + ★ Builder
│   ├── ContractEmployee.java                       — Final subclass + ★ Builder
│   ├── EmployeeFactory.java                        — ★ Factory Method: named employee creation
│   ├── Department.java                             — Record (immutable DTO)
│   ├── PayrollRecord.java                          — Record (immutable DTO)
│   ├── DepartmentSalaryReport.java                 — Record: per-dept salary aggregates
│   ├── SalaryAnalyticsReport.java                  — Record: full analytics bundle (incl. byRole)
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
│   ├── ValidationService.java                      — Interface: throws ValidationException,
│   │                                                  DuplicateEmailException, DepartmentNotFoundException
│   ├── EmployeeValidationService.java              — ★ Full validation: field rules + email uniqueness
│   │                                                  + department-exists; injectable repo + dept set
│   ├── PayrollService.java                         — Interface: payroll processing
│   ├── PayrollServiceImpl.java                     — Payroll orchestration (uses a resolver)
│   ├── PayrollStrategyResolver.java                — Interface: resolves strategy for an employee
│   ├── PayrollStrategyRegistry.java                — ★ Singleton registry/lookup
│   ├── SalaryAnalyticsService.java                 — Interface: stream-based salary analytics
│   └── SalaryAnalyticsServiceImpl.java             — ★ All aggregates via stream pipelines +
│                                                      Comparator chaining in topNBySalary
└── exception/
    ├── EmployeeException.java                      — Base checked exception
    ├── DuplicateEmployeeException.java
    ├── DuplicateEmailException.java
    ├── EmployeeNotFoundException.java
    ├── DepartmentNotFoundException.java             — ★ NEW: departmentId not found
    ├── InvalidEmployeeDataException.java
    ├── PayrollException.java
    ├── ValidationException.java
    └── GlobalExceptionHandler.java                 — ★ NEW: centralised exception → ErrorResponse mapper

src/test/java/com/example/helloworld/
├── controller/
│   ├── EmployeeControllerTest.java                 — Mockito tests for EmployeeController
│   └── PayrollControllerTest.java                  — Mockito tests for PayrollController
├── domain/
│   ├── DepartmentSalaryReportTest.java             — Record aggregates + compact constructor
│   ├── EmployeeSummaryDTOTest.java                 — DTO projection + switch expression
│   └── payroll/
│       └── PayrollStrategyTest.java                — Strategy tax calculations
├── exception/
│   └── GlobalExceptionHandlerTest.java             — ★ NEW: 20 tests for all 7 exception subtypes
└── service/
    ├── EmployeeServiceImplTest.java                — Mockito: delegation + Optional + parametrized
    ├── EmployeeValidationServiceTest.java          — Field-level validation rules + parametrized inputs
    ├── EmployeeValidationServiceEnhancedTest.java  — ★ NEW: 19 tests for email uniqueness,
    │                                                  department-exists, salary rule, ordering
    ├── PayrollServiceImplTest.java                 — Tax calc, batch, null guards, parametrized
    ├── PayrollStrategyRegistryTest.java            — Singleton registry resolution
    ├── SalaryAnalyticsServiceImplTest.java         — 55 tests: parametrized, edge cases, Optional
    └── SalaryAnalyticsServiceImplGroupByRoleTest.java — ★ NEW: 18 tests for groupByRole +
                                                         Comparator chaining / tiebreaker ordering
```

---

## Salary Analytics

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
                                       ├── topNBySalary        → sorted (Comparator chain) + limit
                                       ├── averageSalaryByRole → groupingBy + averagingDouble
                                       ├── partitionByStatus   → Collectors.partitioningBy
                                       └── groupByRole         → groupingBy + sorted within bucket
```

### `SalaryAnalyticsService` — Interface

All methods receive the employee list as a parameter — the service is **stateless** and
has no repository dependency, making it trivially testable.

| Method                                       | Returns                                | Stream operation used                                    |
|----------------------------------------------|----------------------------------------|----------------------------------------------------------|
| `groupByDepartment(employees)`               | `Map<Integer, DepartmentSalaryReport>` | `groupingBy` + `summaryStatistics`                       |
| `topNBySalary(employees, n)`                 | `List<Employee>`                       | `sorted(comparatorChain).limit(n)`                       |
| `averageSalaryByRole(employees)`             | `Map<String, Double>`                  | `groupingBy(role) + averagingDouble(salary)`             |
| `partitionByStatus(employees)`               | `Map<Boolean, List<Employee>>`         | `partitioningBy(status == ACTIVE)`                       |
| `groupByRole(employees)`                     | `Map<String, List<Employee>>`          | `groupingBy(role)` + downstream sort within bucket       |
| `buildReport(employees)`                     | `SalaryAnalyticsReport`                | Calls all five above; bundles into one record            |

### Comparator Chaining in `topNBySalary`

A **three-key chained `Comparator`** is used as a reusable constant throughout the service —
it guarantees a **fully deterministic, total ordering** even when multiple employees share
the same salary:

```java
private static final Comparator<Employee> SALARY_DESC_THEN_NAME_ASC_THEN_ID =
        Comparator.comparingDouble(Employee::getSalary).reversed() // primary:   salary ↓
                  .thenComparing(Employee::getName)                // tiebreaker 1: name ↑
                  .thenComparingInt(Employee::getId);              // tiebreaker 2: id ↑
```

The same comparator is reused inside `groupByRole` to sort employees within each role bucket.

### `SalaryAnalyticsController` — Methods

| Method                  | Returns                                | On error                  |
|-------------------------|----------------------------------------|---------------------------|
| `groupByDepartment()`   | `Map<Integer, DepartmentSalaryReport>` | empty map, logs to stderr |
| `top5BySalary()`        | `List<Employee>`                       | empty list, logs to stderr|
| `averageSalaryByRole()` | `Map<String, Double>`                  | empty map, logs to stderr |
| `partitionByStatus()`   | `Map<Boolean, List<Employee>>`         | both buckets empty        |
| `groupByRole()`         | `Map<String, List<Employee>>`          | empty map, logs to stderr |
| `buildReport()`         | `SalaryAnalyticsReport`                | `null`, logs to stderr    |

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

Bundles all analytics views into a single immutable snapshot.

| Field               | Type                                   | Description                                            |
|---------------------|----------------------------------------|--------------------------------------------------------|
| `byDepartment`      | `Map<Integer, DepartmentSalaryReport>` | Per-department salary summary                          |
| `top5BySalary`      | `List<Employee>`                       | Up to 5 highest earners, Comparator-chain ordered      |
| `avgSalaryByRole`   | `Map<String, Double>`                  | Average salary keyed by lower-cased role               |
| `activeEmployees`   | `List<Employee>`                       | Employees with `ACTIVE` status                         |
| `inactiveEmployees` | `List<Employee>`                       | Employees with `INACTIVE` status                       |
| `byRole`            | `Map<String, List<Employee>>`          | Employees grouped by role, sorted within bucket        |

### Stream Pipeline Design Rules

1. **No loops** — every aggregation uses a stream collector or intermediate operation.
2. **Unmodifiable results** — all returned collections are wrapped via `toUnmodifiableList()`,
   `toUnmodifiableMap()`, or `Collections.unmodifiableMap()`.
3. **Stateless** — `SalaryAnalyticsServiceImpl` has no fields; every method is a pure function.
4. **Key normalisation** — role keys are `.strip().toLowerCase()` so `"Engineer"`,
   `"ENGINEER"`, and `"  engineer  "` all collapse to the same bucket.
5. **Deterministic ordering** — `Comparator` chaining guarantees a stable total order
   with no ties left unresolved.

---

## Validation Logic

### `EmployeeValidationService`

Called by `EmployeeController` before every mutating operation. Three categories of rules
are applied in order:

| # | Category               | Rule                                                   | Exception thrown              |
|---|------------------------|--------------------------------------------------------|-------------------------------|
| 1 | **Field rules**        | `id` must be positive                                  | `ValidationException`         |
| 2 | **Field rules**        | `name` must not be blank                               | `ValidationException`         |
| 3 | **Field rules**        | `email` must not be blank and must contain `@`         | `ValidationException`         |
| 4 | **Field rules**        | `salary` must not be negative                          | `ValidationException`         |
| 5 | **Field rules**        | `status` must not be `null` or `INACTIVE`              | `ValidationException`         |
| 6 | **Field rules**        | `departmentId` must be positive                        | `ValidationException`         |
| 7 | **Field rules**        | `ContractEmployee` contract must not be expired        | `ValidationException`         |
| 8 | **Uniqueness**         | `email` must not belong to a different employee        | `DuplicateEmailException`     |
| 9 | **Referential integrity** | `departmentId` must be in the known-departments set | `DepartmentNotFoundException` |

#### Two Constructors

```java
// Full — enables all three validation categories
new EmployeeValidationService(repository, Set.of(10, 20, 30));

// Field-only — backward-compatible; uniqueness and dept checks skipped
new EmployeeValidationService();
```

The factory wires the full constructor so the live stack validates all three categories.
Unit tests that don't need a full stack use the no-arg constructor.

### `ValidationService` Interface

```java
void validate(Employee employee)
        throws ValidationException, DuplicateEmailException, DepartmentNotFoundException;
```

---

## Exception Hierarchy & Global Exception Handler

### Exception Hierarchy

```
EmployeeException  (base checked)
    ├── DuplicateEmployeeException   — id already exists on add
    ├── DuplicateEmailException      — email already taken on add/update
    ├─��� EmployeeNotFoundException    — id/email not found on update/remove/find
    ├── DepartmentNotFoundException  — departmentId not in known-departments set  ★ NEW
    ├── InvalidEmployeeDataException — field value fails a repository-level rule
    ├── PayrollException             — payroll calculation failure
    └── ValidationException          — employee fails a business rule
```

All exceptions are caught by the **Controller layer** via `GlobalExceptionHandler` — they
never propagate to `App.java`.

### `GlobalExceptionHandler`

A utility class with a single `handle(EmployeeException)` method that maps every subtype to
a structured `ErrorResponse` using `instanceof` pattern matching — **one place to update**
when a new exception subtype is added.

```java
// Any controller method
} catch (EmployeeException e) {
    GlobalExceptionHandler.handleAndLog(e, "EmployeeController");
    //     ↑ dispatches to the correct error code automatically
}
```

#### `ErrorResponse` record

```java
record ErrorResponse(String errorCode, String message, String field) { }
```

| Exception                   | `errorCode`            | `field`         |
|-----------------------------|------------------------|-----------------|
| `DuplicateEmployeeException`| `DUPLICATE_EMPLOYEE_ID`| `id`            |
| `DuplicateEmailException`   | `DUPLICATE_EMAIL`      | `email`         |
| `DepartmentNotFoundException`| `DEPARTMENT_NOT_FOUND`| `departmentId`  |
| `EmployeeNotFoundException` | `EMPLOYEE_NOT_FOUND`   | `null`          |
| `ValidationException`       | `VALIDATION_ERROR`     | field name      |
| `InvalidEmployeeDataException`| `INVALID_DATA`       | field name      |
| `PayrollException`          | `PAYROLL_ERROR`        | `null`          |

#### Methods

| Method                                         | Description                                              |
|------------------------------------------------|----------------------------------------------------------|
| `handle(EmployeeException)`                    | Maps exception → `ErrorResponse`; pure function         |
| `handleAndLog(EmployeeException, String source)` | Same mapping + prints to `System.err` with source label |

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

---

### ★ Singleton — `PayrollStrategyRegistry`

**Problem:** `PayrollServiceImpl` created a brand-new registry and registered both strategies
on every instantiation, wasting allocations and producing inconsistent state.

**Solution:** Uses the **Initialization-on-demand holder** idiom — lazy, thread-safe, no
synchronisation overhead.

```java
PayrollStrategyRegistry registry = PayrollStrategyRegistry.getInstance();
```

**Testability is preserved** — the public constructor is kept so unit tests create
isolated registries without touching the singleton.

---

### ★ Abstract Factory — `ApplicationFactory` / `InMemoryApplicationFactory`

**Problem:** `App.java` manually instantiated every repository, service, and controller with
`new` — knowledge of the entire object graph was hardcoded in the entry point.

**Solution:** `ApplicationFactory` defines an interface for creating a **family of related
objects**. `InMemoryApplicationFactory` wires the in-memory stack, caches instances so all
controllers share the same repository, and wires `EmployeeValidationService` with both the
repository and the valid department set.

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

| Method                               | Returns                      | Notes                                                    |
|--------------------------------------|------------------------------|----------------------------------------------------------|
| `createEmployeeRepository()`         | `EmployeeRepository`         | Cached — same instance per factory                       |
| `createEmployeeService()`            | `EmployeeService`            | Cached — wired to the shared repository                  |
| `createValidationService()`          | `ValidationService`          | Cached — wired with repo + `Set.of(10, 20, 30)`          |
| `createPayrollService()`             | `PayrollService`             | Cached — uses singleton registry                         |
| `createSalaryAnalyticsService()`     | `SalaryAnalyticsService`     | Cached — stateless service                               |
| `createEmployeeController()`         | `EmployeeController`         | New instance each call                                   |
| `createPayrollController()`          | `PayrollController`          | New instance each call                                   |
| `createSalaryAnalyticsController()`  | `SalaryAnalyticsController`  | New instance each call                                   |

---

## Controller Layer

### `EmployeeController`

All commands delegate exception handling to `GlobalExceptionHandler.handleAndLog()` — a
single `catch (EmployeeException e)` per method replaces the previous multi-catch ladder.

| Method                             | Behaviour                                                                                           |
|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `addEmployee(Employee)`            | Validates (field + email uniqueness + dept exists) → adds; all errors mapped via GlobalExceptionHandler |
| `updateEmployee(Employee)`         | Validates → updates; all errors mapped via GlobalExceptionHandler                                   |
| `removeEmployee(int id)`           | Removes; `EmployeeNotFoundException` mapped via GlobalExceptionHandler                              |
| `getById(int id)`                  | Returns `Optional<Employee>`                                                                        |
| `getByEmail(String)`               | Returns `Optional<Employee>`                                                                        |
| `getAllEmployees()`                 | Returns all employees                                                                               |
| `getByDepartment(int)`             | Returns employees in given department                                                               |
| `getByStatus(EmployeeStatus)`      | Returns employees matching status                                                                   |
| `getByRole(String)`                | Case-insensitive partial match on role                                                              |
| `getBySalaryRange(double, double)` | Returns employees in range; empty list on `InvalidEmployeeDataException`                            |
| `countEmployees()`                 | Total number of employees                                                                           |
| `totalSalary()`                    | Sum of all salaries                                                                                 |
| `averageSalary()`                  | Average salary                                                                                      |

### `PayrollController`

| Method                                      | Behaviour                                                                  |
|---------------------------------------------|----------------------------------------------------------------------------|
| `processPayroll(recordId, employee, month)` | Processes one employee; returns `null` on `PayrollException`               |
| `processAll(employees, month)`              | Fault-tolerant batch; returns only successfully processed records          |

### `SalaryAnalyticsController`

See [Salary Analytics](#salary-analytics) section above.

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

### Other Domain Records

| Record                    | Purpose                                               |
|---------------------------|-------------------------------------------------------|
| `Department`              | Immutable department DTO (id, name, location)         |
| `PayrollRecord`           | Immutable payroll result (gross, tax, net, timestamp) |
| `DepartmentSalaryReport`  | Per-dept salary aggregates — see analytics section    |
| `SalaryAnalyticsReport`   | Full analytics bundle incl. `byRole` — see analytics  |

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

---

## Service Layer

### `EmployeeService` / `EmployeeServiceImpl`

Thin delegation layer between controller and repository. Every method maps 1-to-1 to a
repository call, providing an abstraction boundary so the controller never sees
repository types.

### `ValidationService` / `EmployeeValidationService`

See [Validation Logic](#validation-logic) section above.

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

// topNBySalary — Comparator chaining: salary ↓, name ↑, id ↑
employees.stream()
    .sorted(SALARY_DESC_THEN_NAME_ASC_THEN_ID)
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

// groupByRole — groupingBy + downstream sort within each bucket
employees.stream()
    .collect(groupingBy(
            e -> e.getRole().strip().toLowerCase(),
            collectingAndThen(toList(), list -> {
                list.sort(SALARY_DESC_THEN_NAME_ASC_THEN_ID);
                return unmodifiableList(list);
            })
    ));
```

---

## Testing

**250 tests — 0 failures.** Three test patterns applied consistently across all test classes:

### 1. Parameterized Tests (`@ParameterizedTest`)

| Test class                                    | Parameterized coverage                                                           |
|-----------------------------------------------|----------------------------------------------------------------------------------|
| `SalaryAnalyticsServiceImplTest`              | `@CsvSource` for dept aggregates, top-N size, rank order, avg by role, partition counts |
| `SalaryAnalyticsServiceImplGroupByRoleTest`   | `@CsvSource` for per-role head-counts across all 4 roles                         |
| `EmployeeValidationServiceEnhancedTest`       | `@ValueSource` for valid salary boundaries and all known department IDs          |
| `PayrollServiceImplTest`                      | `@CsvSource` for gross/tax/net across salary values for both employee types      |
| `EmployeeServiceImplTest`                     | `@EnumSource` for all statuses, `@ValueSource` for role strings, `@CsvSource` for ranges |
| `EmployeeValidationServiceTest`               | `@ValueSource` for invalid emails and valid salary boundaries                    |
| `GlobalExceptionHandlerTest`                  | `@CsvSource` mapping all 7 exception keys → expected error codes                 |
| `DepartmentSalaryReportTest`                  | `@CsvSource` for min/avg/max across salary pairs                                 |

### 2. Stream & Analytics Edge-Case Tests

| Edge case                                          | Test class                                  |
|----------------------------------------------------|---------------------------------------------|
| Equal salary — tiebreaker by name then id          | `SalaryAnalyticsServiceImplGroupByRoleTest` |
| `n=0`, negative `n` for `topNBySalary`             | `SalaryAnalyticsServiceImplGroupByRoleTest` |
| Role bucket ordered: salary ↓, name ↑             | `SalaryAnalyticsServiceImplGroupByRoleTest` |
| Inner role lists are unmodifiable                  | `SalaryAnalyticsServiceImplGroupByRoleTest` |
| Whitespace role names collapse to same key         | `SalaryAnalyticsServiceImplTest`            |
| Single-employee dept: min == max == avg            | `SalaryAnalyticsServiceImplTest`            |
| All-INACTIVE input: active bucket is empty         | `SalaryAnalyticsServiceImplTest`            |
| 8-employee list: top5 capped at 5                  | `SalaryAnalyticsServiceImplTest`            |
| Empty input throughout all methods                 | `SalaryAnalyticsServiceImplTest`            |

### 3. Validation & Exception Tests

| Scenario                                                        | Test class                              |
|-----------------------------------------------------------------|-----------------------------------------|
| Email uniqueness: no conflict, same-id (update), conflict       | `EmployeeValidationServiceEnhancedTest` |
| Department not found for unknown dept ID                        | `EmployeeValidationServiceEnhancedTest` |
| Salary rule fires before email check (ordering)                 | `EmployeeValidationServiceEnhancedTest` |
| Email format fires before uniqueness (no repo call)             | `EmployeeValidationServiceEnhancedTest` |
| All 7 exception subtypes → correct `errorCode` and `field`      | `GlobalExceptionHandlerTest`            |
| `handleAndLog` returns same `ErrorResponse` as `handle`         | `GlobalExceptionHandlerTest`            |
| `ErrorResponse.of` produces null field                          | `GlobalExceptionHandlerTest`            |
| `handle(null)` throws `IllegalArgumentException`                | `GlobalExceptionHandlerTest`            |

### 4. `Optional` Usage Validation

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

---

## OOP Concepts Applied

### 🏗️ Creational Design Patterns
See the dedicated [Creational Design Patterns](#creational-design-patterns) section above.

### 🏛️ Layered Architecture (Controller → Service → Repository)
- **Controller** — input, validation, exception boundaries (via GlobalExceptionHandler)
- **Service** — business logic, orchestration, stream analytics
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

| Class                    | Strategy                                       |
|--------------------------|------------------------------------------------|
| `Employee`               | Identity by `id`                               |
| `PermanentEmployee`      | Parent equality + `gratuityEligible`           |
| `ContractEmployee`       | Parent equality + `contractEndDate`            |
| `Department`             | Auto-generated by Record (all fields)          |
| `PayrollRecord`          | Auto-generated by Record (all fields)          |
| `DepartmentSalaryReport` | Auto-generated by Record (all fields)          |
| `SalaryAnalyticsReport`  | Auto-generated by Record (all fields)          |
| `EmployeeKey`            | Hand-written — `id` + `email`                  |
| `DepartmentKey`          | Auto-generated by Record — `id` only           |

---

## Build & Run

```bash
# Compile (preview features enabled for sealed switch expressions)
mvn compile

# Run all 250 tests
mvn test

# Run the App demo (Parts 1–5)
mvn exec:java
```

> Requires **Java 17** and **Maven 3.x**.
> `--enable-preview` is configured in `pom.xml` for all three plugins
> (`maven-compiler-plugin`, `maven-surefire-plugin`, `exec-maven-plugin`)
> to support pattern matching in `switch` expressions over sealed hierarchies.
