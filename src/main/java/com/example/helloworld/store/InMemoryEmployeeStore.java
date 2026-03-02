package com.example.helloworld.store;

import com.example.helloworld.model.Employee;
import com.example.helloworld.model.EmployeeStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thread-unsafe in-memory implementation of {@link EmployeeStore}.
 *
 * Collections used:
 *  - HashMap<EmployeeKey, Employee>       : custom object key — O(1) lookup by id+email
 *  - HashMap<Integer, EmployeeKey>        : id → EmployeeKey reverse index for id-only lookups
 *  - HashMap<DepartmentKey, Set<Integer>> : record key — O(1) dept queries
 */
public class InMemoryEmployeeStore implements EmployeeStore {

    // Primary store: composite EmployeeKey → Employee
    private final Map<EmployeeKey, Employee> store = new HashMap<>();

    // Reverse index: id → EmployeeKey (needed to resolve id-only lookups)
    private final Map<Integer, EmployeeKey> idIndex = new HashMap<>();

    // Department index: DepartmentKey (record) → Set of employee ids
    private final Map<DepartmentKey, Set<Integer>> departmentIndex = new HashMap<>();

    // ── helpers ───────────────────────────────────────────────────────────────

    private EmployeeKey keyOf(Employee e) {
        return new EmployeeKey(e.getId(), e.getEmail());
    }

    private DepartmentKey deptKeyOf(Employee e) {
        return new DepartmentKey(e.getDepartmentId(),
                // department name not on Employee — use id as name placeholder for key uniqueness
                String.valueOf(e.getDepartmentId()));
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public void add(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        if (idIndex.containsKey(employee.getId()))
            throw new IllegalArgumentException(
                    "Employee with id " + employee.getId() + " already exists");

        EmployeeKey key = keyOf(employee);
        if (store.containsKey(key))
            throw new IllegalArgumentException(
                    "Employee with email '" + employee.getEmail() + "' already exists");

        store.put(key, employee);
        idIndex.put(employee.getId(), key);
        departmentIndex
                .computeIfAbsent(deptKeyOf(employee), k -> new HashSet<>())
                .add(employee.getId());
    }

    @Override
    public void update(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        EmployeeKey oldKey = idIndex.get(employee.getId());
        if (oldKey == null)
            throw new NoSuchElementException(
                    "No employee found with id " + employee.getId());

        Employee existing = store.get(oldKey);

        EmployeeKey newKey = keyOf(employee);

        // If email changed — remove old key, check new key is not taken, insert new key
        if (!oldKey.equals(newKey)) {
            if (store.containsKey(newKey))
                throw new IllegalArgumentException(
                        "Email '" + employee.getEmail() + "' is already taken");
            store.remove(oldKey);
            idIndex.put(employee.getId(), newKey);
        }

        // If department changed — update department index
        DepartmentKey oldDeptKey = deptKeyOf(existing);
        DepartmentKey newDeptKey = deptKeyOf(employee);
        if (!oldDeptKey.equals(newDeptKey)) {
            Set<Integer> oldSet = departmentIndex.get(oldDeptKey);
            if (oldSet != null) oldSet.remove(employee.getId());
            departmentIndex
                    .computeIfAbsent(newDeptKey, k -> new HashSet<>())
                    .add(employee.getId());
        }

        store.put(newKey, employee);
    }

    @Override
    public void remove(int id) {
        EmployeeKey key = idIndex.remove(id);
        if (key == null)
            throw new NoSuchElementException("No employee found with id " + id);

        Employee existing = store.remove(key);
        if (existing != null) {
            Set<Integer> deptSet = departmentIndex.get(deptKeyOf(existing));
            if (deptSet != null) deptSet.remove(id);
        }
    }

    @Override
    public Optional<Employee> findById(int id) {
        EmployeeKey key = idIndex.get(id);
        return Optional.ofNullable(key != null ? store.get(key) : null);
    }

    @Override
    public List<Employee> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<Employee> findByDepartment(int departmentId) {
        // DepartmentKey (record) as HashMap key — equals/hashCode auto-generated
        DepartmentKey deptKey = new DepartmentKey(departmentId, String.valueOf(departmentId));
        Set<Integer> ids = departmentIndex.getOrDefault(deptKey, Collections.emptySet());
        return ids.stream()
                  .map(idIndex::get)
                  .filter(Objects::nonNull)
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
        // Reconstruct a key with a dummy id=1; email-only lookup isn't possible with
        // composite key — scan via stream (email index could be added if needed)
        return store.values().stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(email.strip()))
                    .findFirst();
    }

    @Override
    public List<Employee> findBySalaryRange(double min, double max) {
        if (min < 0)   throw new IllegalArgumentException("min salary must not be negative");
        if (min > max) throw new IllegalArgumentException("min must be <= max");
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
