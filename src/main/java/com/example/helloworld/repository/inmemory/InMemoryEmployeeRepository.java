package com.example.helloworld.repository.inmemory;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.DepartmentKey;
import com.example.helloworld.repository.EmployeeKey;
import com.example.helloworld.repository.EmployeeRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<EmployeeKey, Employee>        store           = new HashMap<>();
    private final Map<Integer, EmployeeKey>         idIndex         = new HashMap<>();
    private final Map<String, Integer>              emailIndex      = new HashMap<>();
    private final Map<DepartmentKey, Set<Integer>>  departmentIndex = new HashMap<>();

    // ── helpers ───────────────────────────────────────────────────────────────

    private EmployeeKey keyOf(Employee e) {
        return new EmployeeKey(e.getId(), e.getEmail());
    }

    private DepartmentKey deptKeyOf(Employee e) {
        return new DepartmentKey(e.getDepartmentId());
    }

    private static String normaliseEmail(String email) {
        return email.strip().toLowerCase();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void add(Employee employee) throws DuplicateEmployeeException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");
        if (idIndex.containsKey(employee.getId()))
            throw new DuplicateEmployeeException(employee.getId());

        String normEmail = normaliseEmail(employee.getEmail());
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

        String oldNormEmail = normaliseEmail(existing.getEmail());
        String newNormEmail = normaliseEmail(employee.getEmail());
        if (!oldNormEmail.equals(newNormEmail)) {
            if (emailIndex.containsKey(newNormEmail))
                throw new DuplicateEmailException(employee.getEmail());
            emailIndex.remove(oldNormEmail);
            emailIndex.put(newNormEmail, employee.getId());
            store.remove(oldKey);
            idIndex.put(employee.getId(), newKey);
        }

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
            emailIndex.remove(normaliseEmail(existing.getEmail()));
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
        DepartmentKey deptKey = new DepartmentKey(departmentId);
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
        Integer id = emailIndex.get(normaliseEmail(email));
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
        return store.values().stream().mapToDouble(Employee::getSalary).sum();
    }

    @Override
    public double averageSalary() {
        return store.values().stream().mapToDouble(Employee::getSalary).average().orElse(0.0);
    }
}

