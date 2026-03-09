# Java Re-Skill Program — Layered Design with OOP & Creational Design Patterns

## Overview

This project demonstrates a **layered Java application** applying core OOP concepts in Java 17,
including **sealed class hierarchies**, **records**, **encapsulation**, **abstraction**, and
**inheritance**, organised into five distinct layers following the
**Controller → Service → Repository** architecture.

It also applies all five **Creational Design Patterns** (GoF) wherever object construction
complexity justifies them:

| Pattern              | Applied To                                      | Benefit                                                       |
|----------------------|-------------------------------------------------|---------------------------------------------------------------|
| **Builder**          | `PermanentEmployee`, `ContractEmployee`         | Readable, step-by-step construction of complex domain objects |
| **Factory Method**   | `EmployeeFactory`                               | Centralised, named creation hiding concrete subclass details  |
| **Singleton**        | `PayrollStrategyRegistry`                       | One shared, fully-wired strategy registry across the app      |
| **Abstract Factory** | `ApplicationFactory` / `InMemoryApplicationFactory` | Wires the entire controller/service/repo stack in one place  |

---

## Architecture

```
App
 │
 ├── [Abstract Factory] InMemoryApplicationFactory
 │        │
 │        ├── creates EmployeeController  ──►  EmployeeService  ──►  EmployeeRepository
 │        │           │                                                └── InMemoryEmployeeRepository
 │        │           └── (validates via) ValidationService
 │        │
 │        └── creates PayrollController   ──►  PayrollService   ──►  PayrollStrategyResolver
 │                                                                      └── [Singleton] PayrollStrategyRegistry
 │                                                                            ├── PermanentEmployeePayrollStrategy
 │                                                                            └── ContractEmployeePayrollStrategy
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
├── App.java                                        — Entry point / demo runner
├── factory/                                        — ★ Abstract Factory
│   ├── ApplicationFactory.java                     — Abstract factory interface
│   └── InMemoryApplicationFactory.java             — Concrete factory: in-memory stack
├── controller/
│   ├── EmployeeController.java                     — Handles employee requests; catches all exceptions
│   └── PayrollController.java                      — Handles payroll requests; catches all exceptions
├── domain/
│   ├── EmployeeStatus.java                         — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                               — Sealed abstract base class
│   ├── PermanentEmployee.java                      — Final subclass + ★ Builder
│   ├── ContractEmployee.java                       — Final subclass + ★ Builder
│   ├── EmployeeFactory.java                        — ★ Factory Method: named employee creation
│   ├── Department.java                             — Record (immutable DTO)
│   ├── PayrollRecord.java                          — Record (immutable DTO)
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
│   ├── PayrollStrategyResolver.java                — Interface: resolves strategy for an employee (DIP)
│   └── PayrollStrategyRegistry.java                — ★ Singleton registry/lookup (OCP)
└── exception/
    ├── EmployeeException.java                      — Base checked exception
    ├── DuplicateEmployeeException.java
    ├── DuplicateEmailException.java
    ├── EmployeeNotFoundException.java
    ├── InvalidEmployeeDataException.java
    ├── PayrollException.java                       — Payroll calculation failure
    └── ValidationException.java                   — Business rule violation

src/test/java/com/example/helloworld/
├── controller/
│   ├── EmployeeControllerTest.java                 — Mockito tests for EmployeeController
│   └── PayrollControllerTest.java                  — Mockito tests for PayrollController
├── domain/
│   └── payroll/
│       └── PayrollStrategyTest.java
└── service/
    ├── EmployeeServiceImplTest.java
    ├── EmployeeValidationServiceTest.java
    ├── PayrollServiceImplTest.java
    └── PayrollStrategyRegistryTest.java
```

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
// Permanent employee (always ACTIVE, gratuity flag explicit)
Employee alice = EmployeeFactory.createPermanentEmployee(
        1, "Alice Kumar", "alice@example.com",
        10, "Engineer", 85_000,
        LocalDate.of(2020, 6, 1), true);

// Contract employee (always ACTIVE, dates validated internally)
Employee carol = EmployeeFactory.createContractEmployee(
        3, "Carol Menon", "carol@example.com",
        20, "Designer", 60_000,
        LocalDate.of(2023, 1, 1), LocalDate.of(2025, 12, 31));
```

**Adding a new employee type** only requires a new factory method in `EmployeeFactory` — no
existing call-sites change.

---

### ★ Singleton — `PayrollStrategyRegistry`

**Problem:** `PayrollServiceImpl` created a brand-new `PayrollStrategyRegistry` and
registered both strategies on every instantiation, wasting allocations and making the
default registry inconsistent across multiple `PayrollServiceImpl` instances.

**Solution:** `PayrollStrategyRegistry` uses the **Initialization-on-demand holder** idiom —
the most idiomatic, thread-safe, lazy singleton in Java. A single shared instance is
pre-wired with both default strategies and accessed via `getInstance()`.

```java
// One shared, fully-wired instance — lazy, thread-safe
PayrollStrategyRegistry registry = PayrollStrategyRegistry.getInstance();
```

**Testability is preserved** — the public constructor is kept so unit tests can create
isolated, empty registries without touching the singleton:

```java
// In tests — isolated registry, no shared state
PayrollStrategyRegistry registry = new PayrollStrategyRegistry()
        .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy());
```

**Extending the default registry** for a new employee type:

```java
PayrollStrategyRegistry.getInstance()
        .register(MyNewEmployeeType.class, new MyNewPayrollStrategy());
```

---

### ★ Abstract Factory — `ApplicationFactory` / `InMemoryApplicationFactory`

**Problem:** `App.java` manually instantiated every repository, service, and controller with
`new` — knowledge of the entire object graph was hardcoded in the entry point.

**Solution:** `ApplicationFactory` defines an interface for creating a **family of related
objects** (repository → services → controllers). `InMemoryApplicationFactory` is the
concrete implementation that wires the in-memory stack.

```java
// One line to bootstrap the entire application stack
ApplicationFactory factory = new InMemoryApplicationFactory();

EmployeeController empCtrl = factory.createEmployeeController();
PayrollController  payCtrl = factory.createPayrollController();
```

**Swapping the entire stack** (e.g. to a database-backed implementation) requires only
changing the factory:

```java
// Drop-in replacement — no other code changes
ApplicationFactory factory = new DatabaseApplicationFactory();
```

`InMemoryApplicationFactory` caches created instances so that all controllers and services
share the **same** repository instance within one factory context.

| Method                         | Returns                  | Notes                                   |
|--------------------------------|--------------------------|-----------------------------------------|
| `createEmployeeRepository()`   | `EmployeeRepository`     | Cached — same instance per factory      |
| `createEmployeeService()`      | `EmployeeService`        | Cached — wired to the shared repository |
| `createValidationService()`    | `ValidationService`      | Cached                                  |
| `createPayrollService()`       | `PayrollService`         | Cached — uses singleton registry        |
| `createEmployeeController()`   | `EmployeeController`     | New instance each call                  |
| `createPayrollController()`    | `PayrollController`      | New instance each call                  |

---

## Controller Layer

The controller layer sits at the top of the stack. It is the **only** layer that callers
(e.g. `App.java`) interact with directly.

### `EmployeeController`

Wires together `EmployeeService` and `ValidationService`. Validates every mutating
request before it reaches the service. All checked exceptions are caught internally
and printed to stderr — callers never need a `try/catch`.

| Method                             | Behaviour                                                                  |
|------------------------------------|----------------------------------------------------------------------------|
| `addEmployee(Employee)`            | Validates → adds; handles `ValidationException`, `DuplicateEmployeeException`, `DuplicateEmailException` |
| `updateEmployee(Employee)`         | Validates → updates; handles `ValidationException`, `EmployeeNotFoundException`, `DuplicateEmailException` |
| `removeEmployee(int id)`           | Removes; handles `EmployeeNotFoundException`                               |
| `getById(int id)`                  | Returns `Optional<Employee>`                                               |
| `getByEmail(String)`               | Returns `Optional<Employee>`                                               |
| `getAllEmployees()`                 | Returns all employees                                                      |
| `getByDepartment(int)`             | Returns employees in given department                                      |
| `getByStatus(EmployeeStatus)`      | Returns employees matching status                                          |
| `getByRole(String)`                | Case-insensitive partial match on role                                     |
| `getBySalaryRange(double, double)` | Returns employees in range; returns empty list on `InvalidEmployeeDataException` |
| `countEmployees()`                 | Total number of employees                                                  |
| `totalSalary()`                    | Sum of all salaries                                                        |
| `averageSalary()`                  | Average salary                                                             |

### `PayrollController`

Wraps `PayrollService`. Exceptions from payroll calculation are caught and logged to
stderr — callers never need a `try/catch`.

| Method                                        | Behaviour                                                                   |
|-----------------------------------------------|-----------------------------------------------------------------------------|
| `processPayroll(recordId, employee, month)`   | Processes one employee; returns `null` (not a throw) on `PayrollException`  |
| `processAll(employees, month)`                | Fault-tolerant batch; returns successfully processed records                |

---

## Domain Layer

### `Employee` (Sealed Abstract Class)

The base class for all employee types. Uses a **sealed class hierarchy** to restrict subtypes
to only `PermanentEmployee` and `ContractEmployee`.

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

- **Abstract method:** `getEmployeeType()` — forces each subclass to identify itself.
- **`equals()` / `hashCode()`** — identity based on `id`.

---

### `PermanentEmployee` *(extends Employee)*

| Additional Field   | Type      | Description                                  |
|--------------------|-----------|----------------------------------------------|
| `gratuityEligible` | `boolean` | Whether the employee qualifies for gratuity  |

- `getEmployeeType()` returns `"PermanentEmployee"`.
- **Builder:** `PermanentEmployee.builder()` — see [Builder pattern](#-builder----permanentemployee--contractemployee) above.

---

### `ContractEmployee` *(extends Employee)*

| Additional Field  | Type        | Description                         |
|-------------------|-------------|-------------------------------------|
| `contractEndDate` | `LocalDate` | Date the contract expires (mutable) |

- `getEmployeeType()` returns `"ContractEmployee"`.
- `isExpired()` — returns `true` if the contract has passed today's date.
- **Builder:** `ContractEmployee.builder()` — see [Builder pattern](#-builder----permanentemployee--contractemployee) above.

---

### `Department` (Record — Immutable DTO)

| Field      | Type     | Description              |
|------------|----------|--------------------------|
| `id`       | `int`    | Unique department ID     |
| `name`     | `String` | Department name          |
| `location` | `String` | Physical/office location |

---

### `EmployeeStatus` (Enum)

```java
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
```

---

### `PayrollRecord` (Record — Immutable DTO)

| Field                | Type            | Description                               |
|----------------------|-----------------|-------------------------------------------|
| `id`                 | `int`           | Unique payroll record ID                  |
| `employeeId`         | `int`           | Reference to Employee                     |
| `grossSalary`        | `double`        | Total salary before deductions            |
| `taxAmount`          | `double`        | Tax deducted                              |
| `netSalary`          | `double`        | Take-home pay (`grossSalary - taxAmount`) |
| `payrollMonth`       | `LocalDate`     | Month/year the payroll covers             |
| `processedTimestamp` | `LocalDateTime` | Exact date-time the payroll was processed |

- Factory method `PayrollRecord.of(...)` calculates `netSalary` automatically.

---

### Payroll Strategy (`domain/payroll/`)

Implements the **Strategy pattern** — the calculation algorithm is selected at runtime based
on the concrete employee type.

```
PayrollStrategy                       (interface)
    ├── PermanentEmployeePayrollStrategy   — tax rate 20%
    └── ContractEmployeePayrollStrategy    — tax rate 10%; rejects expired contracts
```

#### `PayrollStrategy` (Interface)

```java
PayrollRecord calculate(int recordId, Employee employee, LocalDate payrollMonth)
        throws PayrollException;
```

#### `PermanentEmployeePayrollStrategy`
- Tax rate: **20%** of gross salary.
- Throws `PayrollException` if passed a non-`PermanentEmployee`.

#### `ContractEmployeePayrollStrategy`
- Tax rate: **10%** of gross salary.
- Throws `PayrollException` if the contract has expired (`isExpired() == true`).
- Throws `PayrollException` if passed a non-`ContractEmployee`.

---

## Repository Layer

### `EmployeeRepository` (Interface)

Defines the full data-access contract — all CRUD operations, queries, and aggregations.

```
EmployeeRepository                (interface — repository/)
    └── InMemoryEmployeeRepository    (implementation — repository/inmemory/)
```

### `EmployeeKey` (Custom HashMap Key)

| Rule         | How it's met                                                 |
|--------------|--------------------------------------------------------------|
| `equals()`   | Two keys are equal when **both** `id` **and** `email` match  |
| `hashCode()` | Uses the same two fields — consistent with `equals()`        |
| Immutability | Both fields are `final` — hash never changes after insertion |

### `DepartmentKey` (Record-based HashMap Key)

A **Java Record** used as a `HashMap` key — `equals()`, `hashCode()`, and immutability are
all auto-generated by the compiler.

---

## In-Memory Repository

### `InMemoryEmployeeRepository`

A collections-backed, thread-unsafe implementation of `EmployeeRepository` with **secondary
indexes** for O(1) lookups.

#### Internal Collections

| Field             | Type                                   | Purpose                                   |
|-------------------|----------------------------------------|-------------------------------------------|
| `store`           | `HashMap<EmployeeKey, Employee>`       | Primary store — keyed by composite key    |
| `idIndex`         | `HashMap<Integer, EmployeeKey>`        | id → key reverse index                    |
| `emailIndex`      | `HashMap<String, Integer>`             | email → id — O(1) email uniqueness checks |
| `departmentIndex` | `HashMap<DepartmentKey, Set<Integer>>` | dept → Set of ids — O(1) dept queries     |

#### CRUD Operations

| Method     | Description                                              |
|------------|----------------------------------------------------------|
| `add`      | Adds employee; throws if `id` or `email` already exists  |
| `update`   | Replaces by `id`; maintains all secondary indexes        |
| `remove`   | Removes by `id`; cleans up all indexes                   |
| `findById` | Returns `Optional<Employee>` — O(1)                      |
| `findAll`  | Returns unmodifiable snapshot of all employees           |

#### Query Operations

| Method                  | Description                                      |
|-------------------------|--------------------------------------------------|
| `findByDepartment(int)` | Uses `departmentIndex` — O(1) index lookup       |
| `findByStatus`          | Stream filter over all employees                 |
| `findByRole`            | Case-insensitive partial match via stream filter |
| `findByEmail`           | Uses `emailIndex` — O(1) lookup                  |
| `findBySalaryRange`     | Stream filter with range check                   |

#### Aggregations

| Method            | Description                                       |
|-------------------|---------------------------------------------------|
| `count()`         | Total number of employees                         |
| `totalSalary()`   | Sum of all employee salaries                      |
| `averageSalary()` | Average salary; returns `0.0` if store is empty   |

---

## Service Layer

### `EmployeeService` / `EmployeeServiceImpl`

Defines the business-facing CRUD and query contract. Receives an `EmployeeRepository` via
constructor injection and delegates every operation to it.

```
EmployeeController  →  EmployeeService  →  EmployeeRepository  →  InMemoryEmployeeRepository
```

| Method                             | Description                                          |
|------------------------------------|------------------------------------------------------|
| `addEmployee(Employee)`            | Adds employee; throws on duplicate id or email       |
| `updateEmployee(Employee)`         | Updates employee; throws if not found or email taken |
| `removeEmployee(int id)`           | Removes employee by id; throws if not found          |
| `getById(int id)`                  | Returns `Optional<Employee>`                         |
| `getAllEmployees()`                 | Returns all employees                                |
| `getByDepartment(int)`             | Returns employees in the given department            |
| `getByStatus(EmployeeStatus)`      | Returns employees matching the given status          |
| `getByRole(String)`                | Case-insensitive partial match on role               |
| `getByEmail(String)`               | Returns `Optional<Employee>` by email                |
| `getBySalaryRange(double, double)` | Returns employees within the salary range            |
| `countEmployees()`                 | Total number of employees                            |
| `totalSalary()`                    | Sum of all employee salaries                         |
| `averageSalary()`                  | Average salary; returns `0.0` if no employees        |

---

### `ValidationService` / `EmployeeValidationService`

Validates an `Employee` against business rules before it is persisted.
Called by `EmployeeController` before every mutating operation.
Throws `ValidationException` on the first rule violation found.

| Rule checked                             | Exception message                               |
|------------------------------------------|-------------------------------------------------|
| `id > 0`                                 | must be positive                                |
| `name` not blank                         | must not be blank                               |
| `email` not blank and contains `@`       | must not be blank / must contain '@'            |
| `salary >= 0`                            | must not be negative                            |
| `status` not null and not `INACTIVE`     | cannot add or update an INACTIVE employee       |
| `departmentId > 0`                       | must be positive                                |
| `ContractEmployee.isExpired() == false`  | contract has already expired                    |

---

### `PayrollService` / `PayrollServiceImpl`

Selects the correct `PayrollStrategy` for each employee type and delegates the calculation.

```
PayrollController  →  PayrollService  →  PayrollStrategyResolver  →  PayrollStrategy  →  PayrollRecord
```

| Method                                         | Description                                                               |
|------------------------------------------------|---------------------------------------------------------------------------|
| `processPayroll(recordId, employee, month)`    | Calculates one `PayrollRecord`; throws `PayrollException` on failure      |
| `processAll(employees, month)`                 | Fault-tolerant batch — failed employees are logged to stderr and skipped  |

`PayrollServiceImpl()` (no-arg) uses the **Singleton** `PayrollStrategyRegistry.getInstance()`
as its default resolver. The injected-constructor overload is kept for testing.

Default registration:

| Employee type        | Strategy registered                     | Tax rate |
|----------------------|-----------------------------------------|----------|
| `PermanentEmployee`  | `PermanentEmployeePayrollStrategy`      | 20%      |
| `ContractEmployee`   | `ContractEmployeePayrollStrategy`       | 10%      |

**Extending payroll for a new employee type:**

```java
// Register on the singleton — available app-wide immediately
PayrollStrategyRegistry.getInstance()
        .register(MyNewEmployeeType.class, new MyNewEmployeePayrollStrategy());
```

---

## Exception Layer

All exceptions extend `EmployeeException` (checked), allowing callers to catch either
the broad base type or a specific subtype.
The **Controller layer** catches all of these — they never propagate to `App.java`.

```
EmployeeException  (base)
    ├── DuplicateEmployeeException   — id already exists on add
    ├── DuplicateEmailException      — email already taken on add/update
    ├── EmployeeNotFoundException    — id/email not found on update/remove/find
    ├── InvalidEmployeeDataException — field value fails a repository-level rule
    ├── PayrollException             — payroll calculation failure (expired contract, wrong type)
    └── ValidationException          — employee fails a business validation rule
```

---

## OOP Concepts Applied

### 🏗️ Creational Design Patterns
See the dedicated [Creational Design Patterns](#creational-design-patterns) section above.

### 🏛️ Layered Architecture (Controller → Service → Repository)
Each layer has a **single responsibility** and communicates only with the layer directly below it:
- **Controller** — handles input, validation, and exception boundaries
- **Service** — orchestrates business logic, unaware of HTTP/CLI concerns
- **Repository** — manages data persistence, unaware of business rules

### 🔒 Sealed Class Hierarchy
```
Employee (sealed abstract)
    ├── PermanentEmployee (final)
    └── ContractEmployee  (final)
```
The `sealed` + `permits` keywords restrict the hierarchy to exactly these two subtypes.

### 📦 Encapsulation
All fields in `Employee` are `private`. Only mutable fields (`salary`, `status`,
`contractEndDate`) expose setters, each with validation guards.

### 🎭 Abstraction
- `EmployeeController` — *what* callers can request, hiding service/exception details
- `EmployeeService` — *what* the business layer can do, not *how*
- `EmployeeRepository` — *what* the data layer can do, not *how*
- `PayrollStrategy` — *what* payroll calculation looks like, not *how*
- `PayrollStrategyResolver` — *how* a strategy is selected (separate responsibility from payroll orchestration)
- `ValidationService` — *what* validation means, not *how*
- `ApplicationFactory` — *what* objects to create, not *how* they are wired
- `getEmployeeType()` — abstract in `Employee`, forcing each subclass to identify itself

### 🧬 Inheritance
`PermanentEmployee` and `ContractEmployee` reuse all common fields and validation logic
from `Employee` via `super(...)`, extending it with their own fields and behaviour.

### 🎯 Strategy Pattern
`PayrollStrategy` is selected at runtime. With the resolver/registry in place, new employee
types can be supported by registration without modifying `PayrollServiceImpl`.

### 🟰 `equals()` and `hashCode()`

| Class               | Strategy                                  |
|---------------------|-------------------------------------------|
| `Employee`          | Identity by `id`                          |
| `PermanentEmployee` | Parent equality + `gratuityEligible`      |
| `ContractEmployee`  | Parent equality + `contractEndDate`       |
| `Department`        | Auto-generated by Record (all fields)     |
| `PayrollRecord`     | Auto-generated by Record (all fields)     |
| `EmployeeKey`       | Hand-written — `id` + `email`             |
| `DepartmentKey`     | Auto-generated by Record — `id` only      |

### 📋 Records (DTOs & Keys)
`Department`, `PayrollRecord`, and `DepartmentKey` are **immutable**. Java Records
auto-generate `equals()`, `hashCode()`, `toString()`, and accessor methods,
reducing boilerplate while enforcing immutability.

---

## Build & Run

```bash
# Compile
mvn compile

# Run tests
mvn test

# Run main class
mvn exec:java
```

> Requires **Java 17+** and **Maven 3.x**.
