# Java Re-Skill Program — POJO Classes with OOP Concepts

## Overview

This project demonstrates the creation of POJO (Plain Old Java Object) classes applying core
OOP concepts in Java 17, including **sealed class hierarchies**, **records**, **encapsulation**,
**abstraction**, and **inheritance**.

---

## Project Structure

```
src/main/java/com/example/helloworld/model/
├── EmployeeStatus.java       — Enum: ACTIVE / INACTIVE
├── Employee.java             — Sealed abstract base class
├── PermanentEmployee.java    — Final subclass: permanent staff
├── ContractEmployee.java     — Final subclass: contract staff
├── Department.java           — Record (immutable DTO)
└── PayrollRecord.java        — Record (immutable DTO)
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

