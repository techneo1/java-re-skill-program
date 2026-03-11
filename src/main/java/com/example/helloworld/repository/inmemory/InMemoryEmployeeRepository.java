package com.example.helloworld.repository.inmemory;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.DepartmentKey;
import com.example.helloworld.repository.EmployeeRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<Integer, Employee>           store           = new HashMap<>();
    private final Map<String, Integer>             emailIndex      = new HashMap<>();
    private final Map<DepartmentKey, Set<Integer>> departmentIndex = new HashMap<>();

    // ── helpers ───────────────────────────────────────────────────────────────

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
        if (store.containsKey(employee.getId()))
            throw new DuplicateEmployeeException(employee.getId());

        String normEmail = normaliseEmail(employee.getEmail());
        if (emailIndex.containsKey(normEmail))
            throw new DuplicateEmailException(employee.getEmail());

        store.put(employee.getId(), employee);
        emailIndex.put(normEmail, employee.getId());
        departmentIndex
                .computeIfAbsent(deptKeyOf(employee), k -> new HashSet<>())
                .add(employee.getId());
    }

    @Override
    public void update(Employee employee) throws EmployeeNotFoundException, DuplicateEmailException {
        Objects.requireNonNull(employee, "employee must not be null");
        Employee existing = store.get(employee.getId());
        if (existing == null)
            throw new EmployeeNotFoundException(employee.getId());

        String oldNormEmail = normaliseEmail(existing.getEmail());
        String newNormEmail = normaliseEmail(employee.getEmail());
        if (!oldNormEmail.equals(newNormEmail)) {
            if (emailIndex.containsKey(newNormEmail))
                throw new DuplicateEmailException(employee.getEmail());
            emailIndex.remove(oldNormEmail);
            emailIndex.put(newNormEmail, employee.getId());
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

        store.put(employee.getId(), employee);
    }

    @Override
    public void remove(int id) throws EmployeeNotFoundException {
        Employee existing = store.remove(id);
        if (existing == null)
            throw new EmployeeNotFoundException(id);

        emailIndex.remove(normaliseEmail(existing.getEmail()));
        Set<Integer> deptSet = departmentIndex.get(deptKeyOf(existing));
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
        Set<Integer> ids = departmentIndex.getOrDefault(new DepartmentKey(departmentId), Collections.emptySet());
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
        Integer id = emailIndex.get(normaliseEmail(email));
        return Optional.ofNullable(id != null ? store.get(id) : null);
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
