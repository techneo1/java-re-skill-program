package com.example.helloworld.service;

import com.example.helloworld.domain.Employee;
import com.example.helloworld.domain.EmployeeStatus;
import com.example.helloworld.domain.PermanentEmployee;
import com.example.helloworld.exception.*;
import com.example.helloworld.repository.EmployeeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl")
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeServiceImpl service;

    private PermanentEmployee alice;
    private PermanentEmployee bob;

    // ── Lifecycle hooks ───────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        alice = new PermanentEmployee(1, "Alice Kumar", "alice@example.com",
                10, "Engineer", 85_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2020, 6, 1), true);
        bob = new PermanentEmployee(2, "Bob Singh", "bob@example.com",
                10, "Engineer", 90_000, EmployeeStatus.ACTIVE,
                LocalDate.of(2019, 3, 15), true);
    }

    @AfterEach
    void tearDown() {
        // verifyNoMoreInteractions ensures no unexpected repository calls were made
        verifyNoMoreInteractions(repository);
    }

    // ── addEmployee ───────────────────────────────────────────────────────

    @Test
    @DisplayName("addEmployee — delegates to repository.add")
    void addEmployee_delegatesToRepository() throws Exception {
        service.addEmployee(alice);
        verify(repository).add(alice);
    }

    @Test
    @DisplayName("addEmployee — propagates DuplicateEmployeeException")
    void addEmployee_propagatesDuplicateEmployeeException() throws Exception {
        doThrow(new DuplicateEmployeeException(1)).when(repository).add(alice);
        assertThrows(DuplicateEmployeeException.class, () -> service.addEmployee(alice));
        verify(repository).add(alice);
    }

    @Test
    @DisplayName("addEmployee — propagates DuplicateEmailException")
    void addEmployee_propagatesDuplicateEmailException() throws Exception {
        doThrow(new DuplicateEmailException("alice@example.com")).when(repository).add(alice);
        assertThrows(DuplicateEmailException.class, () -> service.addEmployee(alice));
        verify(repository).add(alice);
    }

    @Test
    @DisplayName("addEmployee — ArgumentCaptor captures the exact employee passed")
    void addEmployee_argumentCaptorCapturesEmployee() throws Exception {
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        service.addEmployee(alice);
        verify(repository).add(captor.capture());
        assertEquals(alice.getId(),    captor.getValue().getId());
        assertEquals(alice.getEmail(), captor.getValue().getEmail());
        assertEquals(alice.getName(),  captor.getValue().getName());
    }

    // ── updateEmployee ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEmployee — delegates to repository.update")
    void updateEmployee_delegatesToRepository() throws Exception {
        service.updateEmployee(alice);
        verify(repository).update(alice);
    }

    @Test
    @DisplayName("updateEmployee — propagates EmployeeNotFoundException")
    void updateEmployee_propagatesNotFoundException() throws Exception {
        doThrow(new EmployeeNotFoundException(99)).when(repository).update(alice);
        assertThrows(EmployeeNotFoundException.class, () -> service.updateEmployee(alice));
        verify(repository).update(alice);
    }

    @Test
    @DisplayName("updateEmployee — propagates DuplicateEmailException")
    void updateEmployee_propagatesDuplicateEmailException() throws Exception {
        doThrow(new DuplicateEmailException("alice@example.com")).when(repository).update(alice);
        assertThrows(DuplicateEmailException.class, () -> service.updateEmployee(alice));
        verify(repository).update(alice);
    }

    // ── removeEmployee ────────────────────────────────────────────────────

    @Test
    @DisplayName("removeEmployee — delegates to repository.remove")
    void removeEmployee_delegatesToRepository() throws Exception {
        service.removeEmployee(1);
        verify(repository).remove(1);
    }

    @Test
    @DisplayName("removeEmployee — propagates EmployeeNotFoundException")
    void removeEmployee_propagatesNotFoundException() throws Exception {
        doThrow(new EmployeeNotFoundException(99)).when(repository).remove(99);
        assertThrows(EmployeeNotFoundException.class, () -> service.removeEmployee(99));
        verify(repository).remove(99);
    }

    // ── getById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — returns present Optional when found")
    void getById_returnsEmployee() {
        when(repository.findById(1)).thenReturn(Optional.of(alice));
        Optional<Employee> result = service.getById(1);
        assertTrue(result.isPresent());
        assertEquals(alice, result.get());
        verify(repository).findById(1);
    }

    @Test
    @DisplayName("getById — returns empty Optional when not found")
    void getById_returnsEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertTrue(service.getById(99).isEmpty());
        verify(repository).findById(99);
    }

    // ── getAllEmployees ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllEmployees — returns all employees from repository")
    void getAllEmployees_returnsList() {
        when(repository.findAll()).thenReturn(List.of(alice, bob));
        List<Employee> result = service.getAllEmployees();
        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("getAllEmployees — returns empty list when store is empty")
    void getAllEmployees_emptyStore() {
        when(repository.findAll()).thenReturn(List.of());
        assertTrue(service.getAllEmployees().isEmpty());
        verify(repository).findAll();
    }

    // ── getByDepartment ───────────────────────────────────────────────────

    @Test
    @DisplayName("getByDepartment — delegates with correct departmentId")
    void getByDepartment_delegatesCorrectly() {
        when(repository.findByDepartment(10)).thenReturn(List.of(alice, bob));
        List<Employee> result = service.getByDepartment(10);
        assertEquals(2, result.size());
        verify(repository).findByDepartment(10);
    }

    // ── getByStatus — Parameterized ───────────────────────────────────────

    @ParameterizedTest(name = "getByStatus({0})")
    @DisplayName("getByStatus — delegates for each EmployeeStatus")
    @org.junit.jupiter.params.provider.EnumSource(EmployeeStatus.class)
    void getByStatus_delegatesForAllStatuses(EmployeeStatus status) {
        when(repository.findByStatus(status)).thenReturn(List.of());
        service.getByStatus(status);
        verify(repository).findByStatus(status);
    }

    // ── getByRole — Parameterized ─────────────────────────────────────────

    @ParameterizedTest(name = "getByRole(\"{0}\")")
    @DisplayName("getByRole — delegates for various role strings")
    @ValueSource(strings = {"engineer", "ENGINEER", "Manager", "qa analyst", ""})
    void getByRole_delegatesWithVariousRoles(String role) {
        when(repository.findByRole(role)).thenReturn(List.of());
        service.getByRole(role);
        verify(repository).findByRole(role);
    }

    // ── getByEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByEmail — returns present Optional when email matches")
    void getByEmail_found() {
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        Optional<Employee> result = service.getByEmail("alice@example.com");
        assertTrue(result.isPresent());
        verify(repository).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("getByEmail — returns empty Optional when email not found")
    void getByEmail_notFound() {
        when(repository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertTrue(service.getByEmail("unknown@example.com").isEmpty());
        verify(repository).findByEmail("unknown@example.com");
    }

    // ── getBySalaryRange — Parameterized ──────────────────────────────────

    @ParameterizedTest(name = "salary range [{0}, {1}]")
    @DisplayName("getBySalaryRange — delegates for valid ranges")
    @org.junit.jupiter.params.provider.CsvSource({
            "0,     100000",
            "50000, 90000",
            "85000, 85000"
    })
    void getBySalaryRange_validRanges(double min, double max) throws Exception {
        when(repository.findBySalaryRange(min, max)).thenReturn(List.of());
        service.getBySalaryRange(min, max);
        verify(repository).findBySalaryRange(min, max);
    }

    @Test
    @DisplayName("getBySalaryRange — propagates InvalidEmployeeDataException")
    void getBySalaryRange_propagatesException() throws Exception {
        when(repository.findBySalaryRange(-1, 100))
                .thenThrow(new InvalidEmployeeDataException("min", -1, "salary must not be negative"));
        assertThrows(InvalidEmployeeDataException.class, () -> service.getBySalaryRange(-1, 100));
        verify(repository).findBySalaryRange(-1, 100);
    }

    // ── Aggregations ──────────────────────────────────────────────────────

    @Test
    @DisplayName("countEmployees — delegates to repository.count")
    void countEmployees_delegates() {
        when(repository.count()).thenReturn(5);
        assertEquals(5, service.countEmployees());
        verify(repository).count();
    }

    @Test
    @DisplayName("totalSalary — delegates to repository.totalSalary")
    void totalSalary_delegates() {
        when(repository.totalSalary()).thenReturn(400_000.0);
        assertEquals(400_000.0, service.totalSalary());
        verify(repository).totalSalary();
    }

    @Test
    @DisplayName("averageSalary — delegates to repository.averageSalary")
    void averageSalary_delegates() {
        when(repository.averageSalary()).thenReturn(80_000.0);
        assertEquals(80_000.0, service.averageSalary());
        verify(repository).averageSalary();
    }

    @Test
    @DisplayName("averageSalary — returns 0.0 when store is empty")
    void averageSalary_emptyStore() {
        when(repository.averageSalary()).thenReturn(0.0);
        assertEquals(0.0, service.averageSalary());
        verify(repository).averageSalary();
    }

    // ── Constructor edge case ─────────────────────────────────────────────

    @Test
    @DisplayName("constructor — throws NullPointerException for null repository")
    void constructor_nullRepository_throws() {
        assertThrows(NullPointerException.class, () -> new EmployeeServiceImpl(null));
    }
}

