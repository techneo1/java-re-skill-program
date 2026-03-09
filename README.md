# Java Re-Skill Program — Layered Design with OOP Concepts

## Overview

This project demonstrates a **layered Java application** applying core OOP concepts in Java 17,
including **sealed class hierarchies**, **records**, **encapsulation**, **abstraction**, and
**inheritance**, organised into five distinct layers following the
**Controller → Service → Repository** architecture:

| Layer          | Package                                      | Responsibility                                        |
|----------------|----------------------------------------------|-------------------------------------------------------|
| **Controller** | `com.example.helloworld.controller`          | Entry point: validates input, delegates to service, handles all exceptions |
| **Service**    | `com.example.helloworld.service`             | Business logic: employee, validation, payroll         |
| **Repository** | `com.example.helloworld.repository`          | Data access contract + key types                      |
| **In-Memory**  | `com.example.helloworld.repository.inmemory` | Collections-backed repository impl                   |
| **Domain**     | `com.example.helloworld.domain`              | Core business entities, value types, payroll strategy |

---

## Architecture

```
App
 │
 ├── EmployeeController  ──►  EmployeeService  ──►  EmployeeRepository  ──►  InMemoryEmployeeRepository
 │        │
 │        └── (validates via) ValidationService
 │
 └── PayrollController   ──►  PayrollService   ──►  PayrollStrategyResolver ──► PayrollStrategy
                                                       ├── PermanentEmployeePayrollStrategy
                                                       └── ContractEmployeePayrollStrategy
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
├── controller/
│   ├── EmployeeController.java                     — Handles employee requests; catches all exceptions
│   └── PayrollController.java                      — Handles payroll requests; catches all exceptions
├── domain/
│   ├── EmployeeStatus.java                         — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                               — Sealed abstract base class
│   ├── PermanentEmployee.java                      — Final subclass: permanent staff
│   ├── ContractEmployee.java                       — Final subclass: contract staff
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
│   └── PayrollStrategyRegistry.java                — Default registry/lookup (OCP)
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

---

### `ContractEmployee` *(extends Employee)*

| Additional Field  | Type        | Description                         |
|-------------------|-------------|-------------------------------------|
| `contractEndDate` | `LocalDate` | Date the contract expires (mutable) |

- `getEmployeeType()` returns `"ContractEmployee"`.
- `isExpired()` — returns `true` if the contract has passed today's date.

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

**Strategy selection (SOLID refactor):**

`PayrollServiceImpl` depends on `PayrollStrategyResolver` (DIP) and the default implementation
is a small registry (`PayrollStrategyRegistry`) (OCP). This removes the `instanceof` chain from
`PayrollServiceImpl`.

Default registration:

| Employee type        | Strategy registered                     | Tax rate |
|----------------------|-----------------------------------------|----------|
| `PermanentEmployee`  | `PermanentEmployeePayrollStrategy`      | 20%      |
| `ContractEmployee`   | `ContractEmployeePayrollStrategy`       | 10%      |

**Extending payroll for a new employee type**

Add a new `PayrollStrategy` implementation and register it:

```java
PayrollStrategyResolver resolver = new PayrollStrategyRegistry()
        .register(PermanentEmployee.class, new PermanentEmployeePayrollStrategy())
        .register(ContractEmployee.class, new ContractEmployeePayrollStrategy())
        .register(MyNewEmployeeType.class, new MyNewEmployeePayrollStrategy());

PayrollService payrollService = new PayrollServiceImpl(resolver);
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
| `DepartmentKey`     | Auto-generated by Record — `id` + `name`  |

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
