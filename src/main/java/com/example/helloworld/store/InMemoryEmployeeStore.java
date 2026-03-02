package com.example.helloworld.store;

import com.example.helloworld.model.Employee;
import com.example.helloworld.model.EmployeeStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thread-unsafe in-memory implementation of {@link EmployeeStore}.
 *
 * Collections used:
 *  - HashMap<Integer, Employee>  : O(1) lookup by id (primary store)
 *  - HashMap<String, Integer>    : email → id index for O(1) email lookup
 *  - HashMap<Integer, Set<Integer>> : departmentId → Set<id> index for fast dept queries
 */
public class InMemoryEmployeeStore implements EmployeeStore {

    // Primary store: id → Employee
    private final Map<Integer, Employee> store = new HashMap<>();

    // Secondary indexes for fast lookups
    private final Map<String, Integer>      emailIndex      = new HashMap<>();
    private final Map<Integer, Set<Integer>> departmentIndex = new HashMap<>();

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public void add(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        if (store.containsKey(employee.getId()))
            throw new IllegalArgumentException(
                    "Employee with id " + employee.getId() + " already exists");
        if (emailIndex.containsKey(employee.getEmail().toLowerCase()))
            throw new IllegalArgumentException(
                    "Employee with email '" + employee.getEmail() + "' already exists");

        store.put(employee.getId(), employee);
        emailIndex.put(employee.getEmail().toLowerCase(), employee.getId());
        departmentIndex
                .computeIfAbsent(employee.getDepartmentId(), k -> new HashSet<>())
                .add(employee.getId());
    }

    @Override
    public void update(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        Employee existing = store.get(employee.getId());
        if (existing == null)
            throw new NoSuchElementException(
                    "No employee found with id " + employee.getId());

        // If email changed, update the email index
        String oldEmail = existing.getEmail().toLowerCase();
        String newEmail = employee.getEmail().toLowerCase();
        if (!oldEmail.equals(newEmail)) {
            if (emailIndex.containsKey(newEmail))
                throw new IllegalArgumentException(
                        "Email '" + employee.getEmail() + "' is already taken");
            emailIndex.remove(oldEmail);
            emailIndex.put(newEmail, employee.getId());
        }

        // If department changed, update the department index
        if (existing.getDepartmentId() != employee.getDepartmentId()) {
            Set<Integer> oldDeptSet = departmentIndex.get(existing.getDepartmentId());
            if (oldDeptSet != null) oldDeptSet.remove(employee.getId());
            departmentIndex
                    .computeIfAbsent(employee.getDepartmentId(), k -> new HashSet<>())
                    .add(employee.getId());
        }

        store.put(employee.getId(), employee);
    }

    @Override
    public void remove(int id) {
        Employee existing = store.remove(id);
        if (existing == null)
            throw new NoSuchElementException("No employee found with id " + id);

        emailIndex.remove(existing.getEmail().toLowerCase());
        Set<Integer> deptSet = departmentIndex.get(existing.getDepartmentId());
        if (deptSet != null) deptSet.remove(id);
    }

    @Override
    public Optional<Employee> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Employee> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<Employee> findByDepartment(int departmentId) {
        Set<Integer> ids = departmentIndex.getOrDefault(departmentId, Collections.emptySet());
        return ids.stream()
                  .map(store::get)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Employee> findByStatus(EmployeeStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return store.values().stream()
                    .filter(e -> e.getStatus() == status)
                    .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Employee> findByRole(String role) {
        Objects.requireNonNull(role, "role must not be null");
        String normalised = role.strip().toLowerCase();
        return store.values().stream()
                    .filter(e -> e.getRole().toLowerCase().contains(normalised))
                    .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        Integer id = emailIndex.get(email.strip().toLowerCase());
        return Optional.ofNullable(id != null ? store.get(id) : null);
    }

    @Override
    public List<Employee> findBySalaryRange(double min, double max) {
        if (min < 0)    throw new IllegalArgumentException("min salary must not be negative");
        if (min > max)  throw new IllegalArgumentException("min must be <= max");
        return store.values().stream()
                    .filter(e -> e.getSalary() >= min && e.getSalary() <= max)
                    .collect(Collectors.toUnmodifiableList());
    }

    // ── Aggregations ──────────────────────────────────────────────────────────

    @Override
    public int count() {
        return store.size();
    }

    @Override
    public double totalSalary() {
        return store.values().stream()
                    .mapToDouble(Employee::getSalary)
                    .sum();
    }

    @Override
    public double averageSalary() {
        return store.values().stream()
                    .mapToDouble(Employee::getSalary)
                    .average()
                    .orElse(0.0);
    }
}

