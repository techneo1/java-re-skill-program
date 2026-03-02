package com.example.helloworld.store;

import com.example.helloworld.exception.*;
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
 *  - HashMap<String, Integer>             : email uniqueness index — O(1) email lookups
 *  - HashMap<DepartmentKey, Set<Integer>> : record key — O(1) dept queries
 */
public class InMemoryEmployeeStore implements EmployeeStore {

    // Primary store: composite EmployeeKey → Employee
    private final Map<EmployeeKey, Employee> store = new HashMap<>();

    // Reverse index: id → EmployeeKey (needed to resolve id-only lookups)
    private final Map<Integer, EmployeeKey> idIndex = new HashMap<>();

    // Email uniqueness index: normalised email → id
    private final Map<String, Integer> emailIndex = new HashMap<>();

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
    public void add(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");
        if (idIndex.containsKey(employee.getId()))
            throw new DuplicateEmployeeException(employee.getId());

        String normEmail = employee.getEmail().strip().toLowerCase();
        if (emailIndex.containsKey(normEmail))
            throw new DuplicateEmailException(employee.getEmail());

        EmployeeKey key = keyOf(employee);
        store.put(key, employee);
        idIndex.put(employee.getId(), key);
        emailIndex.put(normEmail, employee.getId());
        departmentIndex
                .computeIfAbsent(deptKeyOf(employee), k -> new HashSet<>())
                .add(employee.getId());
    }

    @Override
    public void update(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");
        EmployeeKey oldKey = idIndex.get(employee.getId());
        if (oldKey == null)
            throw new EmployeeNotFoundException(employee.getId());

        Employee existing = store.get(oldKey);
        EmployeeKey newKey = keyOf(employee);

        // If email changed — check uniqueness, update emailIndex
        String oldNormEmail = existing.getEmail().strip().toLowerCase();
        String newNormEmail = employee.getEmail().strip().toLowerCase();
        if (!oldNormEmail.equals(newNormEmail)) {
            if (emailIndex.containsKey(newNormEmail))
                throw new DuplicateEmailException(employee.getEmail());
            emailIndex.remove(oldNormEmail);
            emailIndex.put(newNormEmail, employee.getId());
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
    public void remove(int id) throws EmployeeNotFoundException {
        EmployeeKey key = idIndex.remove(id);
        if (key == null)
            throw new EmployeeNotFoundException(id);

        Employee existing = store.remove(key);
        if (existing != null) {
            emailIndex.remove(existing.getEmail().strip().toLowerCase());
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
        Integer id = emailIndex.get(email.strip().toLowerCase());
        if (id == null) return Optional.empty();
        EmployeeKey key = idIndex.get(id);
        return Optional.ofNullable(key != null ? store.get(key) : null);
    }

    @Override
    public List<Employee> findBySalaryRange(double min, double max) throws InvalidEmployeeDataException {
        if (min < 0)   throw new InvalidEmployeeDataException("min", min, "salary must not be negative");
        if (min > max) throw new InvalidEmployeeDataException("min/max", min + "/" + max, "min must be <= max");
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
