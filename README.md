# Java Re-Skill Program — Layered Design with OOP, Creational Patterns, JDBC & Stream Analytics

## Overview

This project demonstrates a **layered Java application** applying core OOP concepts in Java 17,
including **sealed class hierarchies**, **records**, **encapsulation**, **abstraction**, and
**inheritance**, organised into five distinct layers following the
**Controller → Service → Repository** architecture.

It also applies four **Creational Design Patterns** (GoF) wherever object construction
complexity justifies them:

| Pattern              | Applied To                                      | Benefit                                                       |
|----------------------|-------------------------------------------------|---------------------------------------------------------------|
| **Builder**          | `PermanentEmployee`, `ContractEmployee`         | Readable, step-by-step construction of complex domain objects |
| **Factory Method**   | `EmployeeFactory`                               | Centralised, named creation hiding concrete subclass details  |
| **Singleton**        | `PayrollStrategyRegistry`                       | One shared, fully-wired strategy registry across the app      |
| **Abstract Factory** | `ApplicationFactory` / `InMemoryApplicationFactory` / `JdbcApplicationFactory` | Wires the entire controller/service/repo stack in one place |

And two dedicated feature layers:
- **JDBC layer** (`db/`) — `Connection`, `PreparedStatement`, `ResultSet`, and **Transactions**
- **Salary Analytics layer** (`service/SalaryAnalyticsService`) — **Stream pipelines**, **switch expressions** over the sealed hierarchy, and **Records as DTOs**

---

## Architecture

```
App
 │
 ├── [Abstract Factory] InMemoryApplicationFactory          ← in-memory stack (default)
 │        │
 │        ├── creates EmployeeController  ──►  EmployeeService  ──►  EmployeeRepository
 │        │           │                                                └── InMemoryEmployeeRepository
 │        │           └── (validates via) ValidationService
 │        │
 │        ├── creates PayrollController   ──►  PayrollService   ──►  PayrollStrategyResolver
 │        │                                                            └── [Singleton] PayrollStrategyRegistry
 │        │
 │        └── creates SalaryAnalyticsService  ──►  EmployeeService (shared)
 │
 ├── [Abstract Factory] JdbcApplicationFactory              ← JDBC stack (drop-in swap)
 │        │  (same shape as above — repository backed by JdbcEmployeeDao)
 │        └── creates SalaryAnalyticsService  ──►  EmployeeService (shared)
 │
 ├── [JDBC Layer] db/
 │        ├── DataSourceFactory              ← Connection management
 │        ├── JdbcDepartmentDao              ← PreparedStatement + ResultSet (departments table)
 │        ├── JdbcEmployeeDao                ← PreparedStatement + ResultSet (employees table)
 │        ├── JdbcPayrollDao                 ← PreparedStatement + ResultSet (payroll_records table)
 │        └── PayrollTransactionService      ← commit / rollback across multiple DAOs
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
│   ├── InMemoryApplicationFactory.java             — Concrete factory: in-memory stack
│   └── JdbcApplicationFactory.java                 — Concrete factory: JDBC stack (drop-in swap)
├── controller/
│   ├── EmployeeController.java                     — Handles employee requests; catches all exceptions
│   └── PayrollController.java                      — Handles payroll requests; catches all exceptions
├── db/                                             — ★ JDBC Layer
│   ├── DataSourceFactory.java                      — Connection creation, schema DDL bootstrap
│   ├── JdbcDepartmentDao                          — PreparedStatement + ResultSet: departments table
│   ├── JdbcEmployeeDao                        — PreparedStatement + ResultSet: employees table
│   ├── JdbcPayrollDao                         — PreparedStatement + ResultSet: payroll_records table
│   └── PayrollTransactionService              — commit / rollback across JdbcPayrollDao + PayrollService
├── domain/
│   ├── EmployeeStatus.java                         — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                               — Sealed abstract base class
│   ├── PermanentEmployee.java                      — Final subclass + ★ Builder
│   ├── ContractEmployee.java                       — Final subclass + ★ Builder
│   ├── EmployeeFactory.java                        — ★ Factory Method: named employee creation
│   ├── Department.java                             — Record (immutable DTO)
│   ├── PayrollRecord.java                          — Record (immutable DTO)
│   ├── DepartmentSalaryReport.java                 — ★ Record DTO: per-dept salary statistics
│   ├── EmployeeSummaryDTO.java                     — ★ Record DTO: lightweight employee view; from() uses switch expr
│   └── payroll/
│       ├── PayrollStrategy.java                    — Interface: payroll calculation strategy
│       ├── PermanentEmployeePayrollStrategy.java   — Impl: 20% tax for permanent employees
│       └── ContractEmployeePayrollStrategy.java    — Impl: 10% tax, rejects expired contracts
├── repository/
│   ├── EmployeeRepository.java                     — Interface: employee repository contract
│   ├── DepartmentRepository.java                   — Interface: department repository contract
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
│   ├── PayrollStrategyRegistry.java                — ★ Singleton registry/lookup (OCP)
│   ├── SalaryAnalyticsService.java                 — ★ Interface: 5 salary analytics operations
│   └── SalaryAnalyticsServiceImpl.java             — ★ Stream pipeline + switch expr implementation
└── exception/
    ├── EmployeeException.java                      — Base checked exception (extends Exception)
    ├── DuplicateEmployeeException.java
    ├── DuplicateEmailException.java
    ├── EmployeeNotFoundException.java
    ├── InvalidEmployeeDataException.java
    ├── PayrollException.java                       — Payroll calculation failure
    ├── ValidationException.java                   — Business rule violation
    └── DepartmentNotFoundException.java            — Department lookup failure (extends Exception)

src/test/java/com/example/helloworld/
├── controller/
│   ├── EmployeeControllerTest.java
│   └── PayrollControllerTest.java
├── db/                                             — ★ JDBC integration tests (H2 in-memory)
│   ├── DataSourceFactoryTest.java                  — Connection, autoCommit, schema DDL
│   ├── JdbcDepartmentDaoTest.java                  — CRUD + queries on departments table
│   ├── JdbcEmployeeDaoTest.java                    — PreparedStatement / ResultSet, all CRUD + queries
│   ├── JdbcPayrollDaoTest.java                     — save, find, delete, rollback visibility
│   └── PayrollTransactionServiceTest.java          — commit batch, rollback on failure
├── domain/
│   ├── DepartmentSalaryReportTest.java             — ★ Record accessors, equals/hashCode, validation
│   ├── EmployeeSummaryDTOTest.java                 — ★ from() switch expr, field copy, validation
│   └── payroll/
│       └── PayrollStrategyTest.java
└── service/
    ├── EmployeeServiceImplTest.java
    ├── EmployeeValidationServiceTest.java
    ├── EmployeeValidationServiceEnhancedTest.java  — Extended edge-case validation tests
    ├── PayrollServiceImplTest.java
    ├── PayrollStrategyRegistryTest.java
    ├── SalaryAnalyticsServiceImplTest.java         — ★ All 5 analytics: happy paths + edge cases
    ├── SalaryAnalyticsServiceImplGroupByRoleTest.java  — ★ Mixed types, boundary values, tie-breaking
    ├── SalaryAnalyticsServiceImplParameterizedTest.java — ★ Parameterized stream pipeline tests
    └── SalaryAnalyticsServiceImplStreamEdgeCaseTest.java — ★ Stream edge cases (empty, single, ties)
```

---

## Salary Analytics Layer

`SalaryAnalyticsService` + `SalaryAnalyticsServiceImpl` add five read-only analytics
operations on top of the existing `EmployeeService`. All five use **Stream pipelines**
(no explicit loops), **switch expressions** over the sealed `Employee` hierarchy, and
**Records** as immutable result DTOs.

### Analytics DTOs (Records)

#### `DepartmentSalaryReport`

Immutable summary of salary statistics for one department. Produced by `salaryByDepartment()`.

| Field           | Type     | Description                          |
|-----------------|----------|--------------------------------------|
| `departmentId`  | `int`    | Department identifier                |
| `headCount`     | `long`   | Number of employees in department    |
| `totalSalary`   | `double` | Sum of all salaries                  |
| `averageSalary` | `double` | Mean salary                          |
| `minSalary`     | `double` | Lowest individual salary             |
| `maxSalary`     | `double` | Highest individual salary            |

The compact constructor validates all fields (no negative values, `minSalary ≤ maxSalary`).

#### `EmployeeSummaryDTO`

Lightweight immutable view of a single employee. Used in `topNBySalary()` and `partitionByStatus()`.

| Field          | Type             | Description                                        |
|----------------|------------------|----------------------------------------------------|
| `id`           | `int`            | Employee ID                                        |
| `name`         | `String`         | Full name                                          |
| `role`         | `String`         | Job role                                           |
| `departmentId` | `int`            | Department reference                               |
| `salary`       | `double`         | Current salary                                     |
| `status`       | `EmployeeStatus` | `ACTIVE` or `INACTIVE`                             |
| `employeeType` | `String`         | `"PERMANENT"` or `"CONTRACT"` — derived via switch |

The `from(Employee)` factory uses a **switch expression** on the sealed hierarchy:

```java
// Exhaustive — compiler enforces all permits subtypes are handled; no default needed
String employeeType = switch (employee) {
    case PermanentEmployee pe -> "PERMANENT";
    case ContractEmployee  ce -> "CONTRACT";
};
```

---

## JDBC Layer (`db/`)

The `db` package teaches the four foundational JDBC concepts end-to-end using an H2
in-memory database in tests and a swappable `DataSourceFactory` in production.

### Database Schema

Three tables are created automatically by `DataSourceFactory.initSchema()` (DDL uses
`CREATE TABLE IF NOT EXISTS` — safe to call repeatedly). The `departments` table is
created first because `employees` holds a foreign key reference to it:

```sql
departments (
    id       INT          PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(150) NOT NULL
)

employees (
    id                INT          PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(150) NOT NULL UNIQUE,
    department_id     INT          NOT NULL,
    role              VARCHAR(80)  NOT NULL,
    salary            DOUBLE       NOT NULL,
    status            VARCHAR(20)  NOT NULL,   -- 'ACTIVE' | 'INACTIVE'
    joining_date      DATE         NOT NULL,
    employee_type     VARCHAR(20)  NOT NULL,   -- 'PERMANENT' | 'CONTRACT'
    gratuity_eligible BOOLEAN      DEFAULT FALSE,
    contract_end_date DATE         DEFAULT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id)
)

payroll_records (
    id                  INT       PRIMARY KEY,
    employee_id         INT       NOT NULL  REFERENCES employees(id),
    gross_salary        DOUBLE    NOT NULL,
    tax_amount          DOUBLE    NOT NULL,
    net_salary          DOUBLE    NOT NULL,
    payroll_month       DATE      NOT NULL,
    processed_timestamp TIMESTAMP NOT NULL
)
```

---

### 1 · Connection — `DataSourceFactory`

`DataSourceFactory` wraps `DriverManager` and is the single point of connection creation
for the whole `db` layer.

```java
DataSourceFactory dsf = new DataSourceFactory(
        "jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1", "sa", "");

dsf.initSchema();                        // creates all three tables (IF NOT EXISTS)

// Plain connection — autoCommit=true (each statement auto-committed)
try (Connection con = dsf.getConnection()) {
    // ... use con
}

// Transactional connection — autoCommit=false, caller must commit/rollback
try (Connection con = dsf.getTransactionalConnection()) {
    // ... use con
    con.commit();
}
```

| Method                        | autoCommit | Use case                                     |
|-------------------------------|------------|----------------------------------------------|
| `getConnection()`             | `true`     | Single-statement operations                  |
| `getTransactionalConnection()`| `false`    | Multi-statement transactions (commit/rollback)|
| `initSchema()`                | `true`     | DDL bootstrap — idempotent (`IF NOT EXISTS`) |

---

### 2 · PreparedStatement — `JdbcDepartmentDao`, `JdbcEmployeeDao` & `JdbcPayrollDao`

Every DML statement and every parameterised query uses a `PreparedStatement`.
**`Statement` is never used for user-supplied data** (only for schema DDL).

```java
// INSERT — bind by position, execute
try (Connection con = dsf.getConnection();
     PreparedStatement ps = con.prepareStatement(
         "INSERT INTO employees (id, name, email, ...) VALUES (?, ?, ?, ...)")) {

    ps.setInt(1,    employee.getId());      // ← typed positional binding
    ps.setString(2, employee.getName());
    ps.setString(3, employee.getEmail());
    ps.setDouble(6, employee.getSalary());
    ps.setDate(8,   Date.valueOf(employee.getJoiningDate()));   // java.time → java.sql

    ps.executeUpdate();   // returns rows affected
}

// UPDATE — check rows affected to detect "not found"
int rows = ps.executeUpdate();
if (rows == 0) throw new EmployeeNotFoundException(id);

// DELETE — same pattern
ps.setInt(1, id);
int deleted = ps.executeUpdate();
```

**Key benefits of `PreparedStatement` over `Statement`:**
- Prevents SQL injection — parameters are never interpreted as SQL
- DB can pre-compile the query plan — better performance on repeated calls
- Typed setters (`setInt`, `setString`, `setDate`, …) make intent explicit

---

### 3 · ResultSet — `JdbcDepartmentDao`, `JdbcEmployeeDao` & `JdbcPayrollDao`

`ps.executeQuery()` returns a `ResultSet`. The cursor starts **before the first row** and
must be advanced with `rs.next()`.

```java
// Single row — if (rs.next())
try (ResultSet rs = ps.executeQuery()) {
    if (rs.next()) {                          // advances cursor; false = no row
        int    id    = rs.getInt("id");       // read column by name
        String name  = rs.getString("name");
        double sal   = rs.getDouble("salary");
        LocalDate d  = rs.getDate("joining_date").toLocalDate();  // java.sql → java.time
    }
}

// Multiple rows — while (rs.next())
List<Employee> results = new ArrayList<>();
try (ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {                       // iterate until no more rows
        results.add(mapRow(rs));
    }
}
```

**`java.time` ↔ `java.sql` type conversions:**

| Java type      | JDBC write                           | JDBC read                                  |
|----------------|--------------------------------------|--------------------------------------------|
| `LocalDate`    | `Date.valueOf(localDate)`            | `rs.getDate("col").toLocalDate()`          |
| `LocalDateTime`| `Timestamp.valueOf(localDateTime)`   | `rs.getTimestamp("col").toLocalDateTime()` |

---

### 4 · Transactions — `PayrollTransactionService`

A **transaction** groups multiple SQL statements so they all succeed or all fail atomically.
`PayrollTransactionService` wraps a complete payroll batch in a single transaction.

```java
// JDBC transaction recipe
try (Connection con = dsf.getTransactionalConnection()) {  // autoCommit=false → start txn
    try {
        for (Employee emp : employees) {
            PayrollRecord record = payrollService.processPayroll(recordId++, emp, month);
            payrollDao.save(con, record);  // ← uses shared connection (same transaction)
        }
        con.commit();    // ← all INSERTs become permanent atomically

    } catch (Exception e) {
        con.rollback();  // ← all INSERTs are discarded — as if they never happened
        throw ...;
    }
}
```

| Scenario                              | Result                                          |
|---------------------------------------|-------------------------------------------------|
| All employees succeed                 | `commit()` — every record persisted             |
| Any calculation or DB error mid-batch | `rollback()` — zero records persisted           |

**Contrast with `PayrollServiceImpl.processAll()`** — that method is *fault-tolerant*
(skip-on-error, persists partial results). `PayrollTransactionService` is *all-or-nothing*.

The connection-sharing pattern is key: `JdbcPayrollDao.save(Connection, record)` accepts
a caller-supplied connection so multiple saves can be enlisted in **the same transaction**
without the DAO needing to know about transaction scope.

```java
// Standalone usage (JdbcApplicationFactory wires this for you)
PayrollTransactionService txService = new PayrollTransactionService(
        dsf, payrollService, payrollDao);

// Atomic batch — commit or rollback together
List<PayrollRecord> records = txService.processAndSaveAll(employees, LocalDate.now());

// Single record — still wrapped in its own transaction
PayrollRecord record = txService.processAndSave(1, alice, LocalDate.now());
```

---

### ★ Abstract Factory — `ApplicationFactory` / `InMemoryApplicationFactory` / `JdbcApplicationFactory`

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
// In-memory stack (default — no DB required)
ApplicationFactory factory = new InMemoryApplicationFactory();

// JDBC stack — full SQL persistence, same controller/service API
ApplicationFactory factory = new JdbcApplicationFactory(
        "jdbc:h2:mem:hrdb;DB_CLOSE_DELAY=-1", "sa", "");
```

`JdbcApplicationFactory` calls `DataSourceFactory.initSchema()` in its constructor, so
tables are created automatically the first time it is instantiated.

| Factory                      | Repository                   | Persistence       |
|------------------------------|------------------------------|-------------------|
| `InMemoryApplicationFactory` | `InMemoryEmployeeRepository` | JVM heap only     |
| `JdbcApplicationFactory`     | `JdbcEmployeeDao`            | SQL database (JDBC)|

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
- **Builder:** `PermanentEmployee.builder()` — see [Builder pattern](#️-creational-design-patterns) above.

---

### `ContractEmployee` *(extends Employee)*

| Additional Field  | Type        | Description                         |
|-------------------|-------------|-------------------------------------|
| `contractEndDate` | `LocalDate` | Date the contract expires (mutable) |

- `getEmployeeType()` returns `"ContractEmployee"`.
- `isExpired()` — returns `true` if the contract has passed today's date.
- **Builder:** `ContractEmployee.builder()` — see [Builder pattern](#️-creational-design-patterns) above.

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

### `DepartmentSalaryReport` (Record — Analytics DTO)

Immutable result of `salaryByDepartment()`. See the [Salary Analytics](#salary-analytics-layer) section for full field documentation.

- Compact constructor validates all numeric invariants (`minSalary ≤ maxSalary`, no negatives).
- Auto-generated `equals()`, `hashCode()`, and `toString()` by the Java Record mechanism.

---

### `EmployeeSummaryDTO` (Record — Analytics DTO)

Lightweight immutable projection of an `Employee`. Built via `EmployeeSummaryDTO.from(Employee)`.

- `from()` uses a **switch expression** over the **sealed `Employee` hierarchy** to set `employeeType`.
- Compact constructor validates all fields (non-blank strings, non-negative salary, non-null status).
- Auto-generated `equals()`, `hashCode()`, and `toString()` by the Java Record mechanism.

---

## Repository Layer

### `EmployeeRepository` (Interface)

Defines the full data-access contract — all CRUD operations, queries, and aggregations.

```
EmployeeRepository                (interface — repository/)
    └── InMemoryEmployeeRepository    (implementation — repository/inmemory/)
    └── JdbcEmployeeDao               (implementation — db/)
```

### `DepartmentRepository` (Interface)

Defines the data-access contract for `Department` persistence.

| Method                    | Description                                                          |
|---------------------------|----------------------------------------------------------------------|
| `add(Department)`         | Inserts a new department; throws `IllegalArgumentException` if id already exists |
| `update(Department)`      | Updates name and location; throws `DepartmentNotFoundException` if not found |
| `remove(int id)`          | Deletes by id; throws `DepartmentNotFoundException` if not found     |
| `findById(int)`           | Returns `Optional<Department>`                                       |
| `findByName(String)`      | Case-insensitive exact match; returns `Optional<Department>`         |
| `findAll()`               | Returns all departments ordered by id                                |
| `findByLocation(String)`  | Case-insensitive match on location; returns list ordered by id       |
| `count()`                 | Total number of departments                                          |

```
DepartmentRepository              (interface — repository/)
    └── JdbcDepartmentDao             (implementation — db/)
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
        .register(MyNewEmployeeType.class, new MyNewPayrollStrategy());
```

---

### `SalaryAnalyticsService` / `SalaryAnalyticsServiceImpl`

Read-only salary analytics over the full employee roster. Delegates data access entirely
to `EmployeeService` — no direct repository dependency.

```
SalaryAnalyticsService (interface)
    └── SalaryAnalyticsServiceImpl   — Stream pipelines + switch expressions
```

| Method                         | Stream API used                                   | Returns                                       |
|--------------------------------|---------------------------------------------------|-----------------------------------------------|
| `salaryByDepartment()`         | `groupingBy` + `summarizingDouble`                | `Map<Integer, DepartmentSalaryReport>` sorted |
| `topNBySalary(n)`              | `sorted` + `limit` + `map`                        | `List<EmployeeSummaryDTO>` descending         |
| `top5BySalary()`               | default — delegates to `topNBySalary(5)`          | `List<EmployeeSummaryDTO>`                    |
| `averageSalaryByRole()`        | `groupingBy` + `averagingDouble`                  | `Map<String, Double>` sorted by role name     |
| `partitionByStatus()`          | `partitioningBy` + `mapping`                      | `Map<Boolean, List<EmployeeSummaryDTO>>`      |
| `averageSalaryByEmployeeType()`| `groupingBy(switch expr)` + `averagingDouble`     | `Map<String, Double>` sorted by type name     |

---

## Exception Layer

### `EmployeeException` hierarchy (checked — extends `Exception`)

All employee-domain exceptions extend `EmployeeException`, allowing callers to catch either
the broad base type or a specific subtype.
The **Controller layer** catches all of these — they never propagate to `App.java`.

```
EmployeeException  (base — extends Exception)
    ├── DuplicateEmployeeException   — id already exists on add
    ├── DuplicateEmailException      — email already taken on add/update
    ├── EmployeeNotFoundException    — id/email not found on update/remove/find
    ├── InvalidEmployeeDataException — field value fails a repository-level rule
    ├── PayrollException             — payroll calculation failure (expired contract, wrong type)
    └── ValidationException          — employee fails a business validation rule
```

### `DepartmentNotFoundException` (checked — extends `Exception` directly)

Thrown when a department lookup by `id` or `name` finds no match. Stands alone outside
the `EmployeeException` hierarchy because department operations are independent of employee
operations.

| Constructor                         | Message produced                          |
|-------------------------------------|-------------------------------------------|
| `DepartmentNotFoundException(int)`  | `"No department found with id: <id>"`     |
| `DepartmentNotFoundException(String)`| `"No department found with name: <name>"`|

`getSearchKey()` returns the id or name that was searched, as a `String`.

---

## OOP Concepts Applied

### 🏗️ Creational Design Patterns

#### ★ Builder — `PermanentEmployee` & `ContractEmployee`

Both concrete employee types expose a static `builder()` method. The builder enforces
mandatory fields and provides a fluent API for optional ones.

```java
PermanentEmployee emp = PermanentEmployee.builder()
        .id(1).name("Alice").email("alice@example.com")
        .departmentId(10).role("Engineer").salary(90_000)
        .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.of(2022, 1, 1))
        .gratuityEligible(true)
        .build();
```

#### ★ Factory Method — `EmployeeFactory`

Named static methods centralise employee creation and hide which concrete subclass is
being instantiated:

```java
Employee perm = EmployeeFactory.createPermanentEmployee(1, "Alice", ...);
Employee cont = EmployeeFactory.createContractEmployee(2, "Bob",   ...);
```

#### ★ Singleton — `PayrollStrategyRegistry`

`PayrollStrategyRegistry.getInstance()` returns the single shared instance. Strategies are
registered once at startup and resolved by any component that holds a reference to the
registry.

#### ★ Abstract Factory — `ApplicationFactory`

`InMemoryApplicationFactory` and `JdbcApplicationFactory` each wire a complete, consistent
object graph. Switching stacks is a one-line change at the entry point.

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
- `EmployeeRepository` / `DepartmentRepository` — *what* the data layer can do, not *how*
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

### 🌊 Stream Pipelines (no explicit loops)
Every analytics operation in `SalaryAnalyticsServiceImpl` is a single declarative
Stream pipeline — no `for`/`while` loops anywhere in the analytics layer.

| Collector used         | Where                              | What it does                              |
|------------------------|------------------------------------|-------------------------------------------|
| `groupingBy`           | `salaryByDepartment`, `averageSalaryByRole`, `averageSalaryByEmployeeType` | Groups elements by a key function |
| `summarizingDouble`    | `salaryByDepartment`               | count + sum + avg + min + max in one pass |
| `averagingDouble`      | `averageSalaryByRole`, `averageSalaryByEmployeeType` | Mean of a double-valued field  |
| `partitioningBy`       | `partitionByStatus`                | Always produces `{true:[...], false:[...]}` |
| `mapping`              | `partitionByStatus`                | Projects each element before collecting  |
| `sorted` + `limit`     | `topNBySalary`                     | Top-N without loading all into memory    |

### 🔀 Switch Expressions on Sealed Hierarchies
The sealed `Employee` hierarchy (`PermanentEmployee` | `ContractEmployee`) is matched
exhaustively in switch expressions — no `default` branch, and a missing subtype is a
**compile error**, not a runtime gap.

```java
// In EmployeeSummaryDTO.from() — converts an Employee to a typed DTO
String tag = switch (employee) {
    case PermanentEmployee pe -> "PERMANENT";
    case ContractEmployee  ce -> "CONTRACT";
};

// In SalaryAnalyticsServiceImpl.averageSalaryByEmployeeType() — used as a grouping classifier
emp -> switch (emp) {
    case PermanentEmployee pe -> "PERMANENT";
    case ContractEmployee  ce -> "CONTRACT";
}
```

### 📋 Records (DTOs & Keys)
`Department`, `PayrollRecord`, `DepartmentKey`, **`DepartmentSalaryReport`**, and
**`EmployeeSummaryDTO`** are all **immutable**. Java Records auto-generate
`equals()`, `hashCode()`, `toString()`, and accessor methods, eliminating boilerplate
while enforcing immutability.

| Record                   | Used as                  | Auto-generates              |
|--------------------------|--------------------------|-----------------------------|
| `Department`             | Domain entity DTO        | equals, hashCode, toString  |
| `PayrollRecord`          | Payroll result DTO       | equals, hashCode, toString  |
| `DepartmentKey`          | HashMap key              | equals, hashCode            |
| `DepartmentSalaryReport` | Analytics result DTO     | equals, hashCode, toString  |
| `EmployeeSummaryDTO`     | Analytics result DTO     | equals, hashCode, toString  |

---

## Build & Run

### Prerequisites

| Tool    | Version   |
|---------|-----------|
| Java    | 17+       |
| Maven   | 3.x       |

### Dependencies

| Dependency                  | Version  | Scope  | Purpose                          |
|-----------------------------|----------|--------|----------------------------------|
| `junit-jupiter`             | 5.10.2   | test   | JUnit 5 test framework           |
| `mockito-core`              | 5.11.0   | test   | Mocking framework                |
| `mockito-junit-jupiter`     | 5.11.0   | test   | Mockito ↔ JUnit 5 integration    |
| `h2`                        | 2.2.224  | test   | In-memory SQL database (JDBC tests) |

### Commands

```bash
# Compile  (--enable-preview required for pattern matching in switch on Java 17)
mvn compile

# Run all tests
mvn test

# Run tests + generate JaCoCo coverage report → target/site/jacoco/index.html
mvn test jacoco:report

# Run main class
mvn exec:java
```

> `--enable-preview` is configured in `pom.xml` for both the `maven-compiler-plugin` and
> `maven-surefire-plugin` to enable pattern matching in switch expressions on Java 17.
