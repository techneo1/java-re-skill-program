# Java Re-Skill Program — POJO Classes with OOP Concepts

## Overview

This project demonstrates the creation of POJO (Plain Old Java Object) classes applying core
OOP concepts in Java 17, including **sealed class hierarchies**, **records**, **encapsulation**,
**abstraction**, and **inheritance**.

---

## Project Structure

```
src/main/java/com/example/helloworld/
├── App.java                            — Entry point / demo runner
├── model/
│   ├── EmployeeStatus.java             — Enum: ACTIVE / INACTIVE
│   ├── Employee.java                   — Sealed abstract base class
│   ├── PermanentEmployee.java          — Final subclass: permanent staff
│   ├── ContractEmployee.java           — Final subclass: contract staff
│   ├── Department.java                 — Record (immutable DTO)
│   └── PayrollRecord.java              — Record (immutable DTO)
└── store/
    ├── EmployeeStore.java              — Interface: store contract
    └── InMemoryEmployeeStore.java      — Implementation: collections-backed store
```

---

## Classes

### 1. `Employee` (Sealed Abstract Class)

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

### 2. `PermanentEmployee` *(extends Employee)*

Represents a full-time permanent employee.

| Additional Field    | Type      | Description                            |
|---------------------|-----------|----------------------------------------|
| `gratuityEligible`  | `boolean` | Whether the employee qualifies for gratuity |

- `getEmployeeType()` returns `"PermanentEmployee"`.
- `equals()` / `hashCode()` extend the parent's and include `gratuityEligible`.

---

### 3. `ContractEmployee` *(extends Employee)*

Represents a contract-based employee with a fixed end date.

| Additional Field   | Type        | Description                        |
|--------------------|-------------|------------------------------------|
| `contractEndDate`  | `LocalDate` | Date the contract expires (mutable)|

- `getEmployeeType()` returns `"ContractEmployee"`.
- `isExpired()` — returns `true` if the contract has passed today's date.
- `equals()` / `hashCode()` extend the parent's and include `contractEndDate`.

---

### 4. `Department` (Record — Immutable DTO)

Represents an organizational department.

| Field      | Type     | Description              |
|------------|----------|--------------------------|
| `id`       | `int`    | Unique department ID     |
| `name`     | `String` | Department name          |
| `location` | `String` | Physical/office location |

- Implemented as a **Java Record** — `equals()`, `hashCode()`, and `toString()` are auto-generated.
- Compact constructor validates that `id > 0`, `name` and `location` are non-null and non-blank.

---

### 5. `EmployeeStatus` (Enum)

```java
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
```

---

### 6. `PayrollRecord` (Record — Immutable DTO)

Represents a single payroll processing record for an employee.

| Field                | Type            | Description                               |
|----------------------|-----------------|-------------------------------------------|
| `id`                 | `int`           | Unique payroll record ID                  |
| `employeeId`         | `int`           | Reference to Employee                     |
| `grossSalary`        | `double`        | Total salary before deductions            |
| `taxAmount`          | `double`        | Tax deducted                              |
| `netSalary`          | `double`        | Take-home pay (`grossSalary - taxAmount`) |
| `payrollMonth`       | `LocalDate`     | Month/year the payroll covers             |
| `processedTimestamp` | `LocalDateTime` | Exact date-time the payroll was processed |

- Implemented as a **Java Record** — `equals()`, `hashCode()`, and `toString()` are auto-generated.
- Compact constructor validates consistency: `netSalary == grossSalary - taxAmount`.
- **Factory method** `PayrollRecord.of(...)` — calculates `netSalary` automatically so callers don't have to.

---

## In-Memory Employee Store

### `EmployeeStore` (Interface)

Defines the full contract for the store — all CRUD operations and query methods.

```
EmployeeStore  (interface)
    └── InMemoryEmployeeStore  (implementation)
```

### `InMemoryEmployeeStore` (Implementation)

A collections-backed, in-memory store with **secondary indexes** for fast lookups.

#### Internal Collections

| Collection                          | Type                          | Purpose                                      |
|-------------------------------------|-------------------------------|----------------------------------------------|
| `store`                             | `HashMap<Integer, Employee>`  | Primary store — O(1) lookup by `id`          |
| `emailIndex`                        | `HashMap<String, Integer>`    | email → id index — O(1) lookup by email      |
| `departmentIndex`                   | `HashMap<Integer, Set<Integer>>` | departmentId → Set of ids — O(1) dept query |

#### CRUD Operations

| Method                    | Description                                              |
|---------------------------|----------------------------------------------------------|
| `add(Employee)`           | Adds employee; throws if `id` or `email` already exists  |
| `update(Employee)`        | Replaces by `id`; maintains email & dept indexes         |
| `remove(int id)`          | Removes by `id`; cleans up all indexes                   |
| `findById(int id)`        | Returns `Optional<Employee>` — O(1)                      |
| `findAll()`               | Returns unmodifiable snapshot of all employees           |

#### Query Operations

| Method                                    | Description                                          |
|-------------------------------------------|------------------------------------------------------|
| `findByDepartment(int departmentId)`      | Uses `departmentIndex` — O(1) for index lookup       |
| `findByStatus(EmployeeStatus)`            | Stream filter over all employees                     |
| `findByRole(String)`                      | Case-insensitive partial match via stream filter      |
| `findByEmail(String)`                     | Uses `emailIndex` — O(1) lookup                      |
| `findBySalaryRange(double min, double max)` | Stream filter with range check                     |

#### Aggregations

| Method            | Description                                     |
|-------------------|-------------------------------------------------|
| `count()`         | Total number of employees                       |
| `totalSalary()`   | Sum of all employee salaries                    |
| `averageSalary()` | Average salary; returns `0.0` if store is empty |

---

## Sample Output (`mvn exec:java`)

```
── All Employees (5) ──────────────────────────────────────
PermanentEmployee{id=1, name='Alice Kumar', ..., salary=85000.00, status=ACTIVE, ...}
PermanentEmployee{id=2, name='Bob Singh',   ..., salary=90000.00, status=ACTIVE, ...}
ContractEmployee {id=3, name='Carol Menon', ..., salary=60000.00, status=ACTIVE, ...}
PermanentEmployee{id=4, name='Dave Patel',  ..., salary=110000.00, status=ACTIVE, ...}
ContractEmployee {id=5, name='Eve Sharma',  ..., salary=55000.00, status=INACTIVE, ...}

── Find by id = 3 ─────────────────────────────────────────
ContractEmployee{id=3, name='Carol Menon', ...}

── Find by email = dave@example.com ───────────────────────
PermanentEmployee{id=4, name='Dave Patel', ...}

── Employees in Department 10 ─────────────────────────────
PermanentEmployee{id=1, name='Alice Kumar', ...}
PermanentEmployee{id=2, name='Bob Singh', ...}

── INACTIVE Employees ─────────────────────────────────────
ContractEmployee{id=5, name='Eve Sharma', ...}

── Employees with role containing 'engineer' ──────────────
PermanentEmployee{id=1, name='Alice Kumar', ...}
PermanentEmployee{id=2, name='Bob Singh', ...}

── Employees with salary between 60,000 and 95,000 ────────
PermanentEmployee{id=1, name='Alice Kumar', salary=85000.00}
PermanentEmployee{id=2, name='Bob Singh',   salary=90000.00}
ContractEmployee {id=3, name='Carol Menon', salary=60000.00}

── Aggregations ───────────────────────────────────────────
  Total employees : 5
  Total salary    : 400000.00
  Average salary  : 80000.00

── Update Alice's salary to 95,000 ────────────────────────
PermanentEmployee{id=1, name='Alice Kumar', salary=95000.00, ...}

── Remove Eve (id=5) ──────────────────────────────────────
Store size after removal: 4
Find Eve by id: Optional.empty
```

---

## OOP Concepts Applied

### 🔒 Sealed Class Hierarchy
```
Employee (sealed abstract)
    ├── PermanentEmployee (final)
    └── ContractEmployee  (final)
```
The `sealed` + `permits` keywords restrict the hierarchy to exactly these two subtypes.
Both subclasses are `final`, fully closing the hierarchy.

### 📦 Encapsulation
All fields in `Employee` are `private`. Only mutable fields (`salary`, `status`,
`contractEndDate`) expose setters, each with validation guards.

### 🎭 Abstraction
`getEmployeeType()` is declared `abstract` in `Employee`, forcing each concrete subclass
to provide its own implementation.

### 🧬 Inheritance
`PermanentEmployee` and `ContractEmployee` reuse all common fields and validation logic
from `Employee` through `super(...)`, and extend it with their own fields and behaviour.

### 🟰 `equals()` and `hashCode()`

| Class                | Strategy                                               |
|----------------------|--------------------------------------------------------|
| `Employee`           | Identity by `id`                                       |
| `PermanentEmployee`  | Parent equality + `gratuityEligible`                   |
| `ContractEmployee`   | Parent equality + `contractEndDate`                    |
| `Department`         | Auto-generated by Record (all fields)                  |
| `PayrollRecord`      | Auto-generated by Record (all fields)                  |

### 📋 Records (DTOs)
`Department` and `PayrollRecord` are **immutable data carriers**. Java Records
auto-generate `equals()`, `hashCode()`, `toString()`, and accessor methods,
reducing boilerplate while enforcing immutability.

---

## Build & Run

```bash
# Compile
mvn compile

# Run main class
mvn exec:java
```

> Requires **Java 17+** and **Maven 3.x**.
