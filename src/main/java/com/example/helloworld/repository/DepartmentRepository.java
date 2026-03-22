package com.example.helloworld.repository;

import com.example.helloworld.domain.Department;
import com.example.helloworld.exception.DepartmentNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for {@link Department} persistence.
 */
public interface DepartmentRepository {

    /**
     * Inserts a new department.
     *
     * @throws IllegalArgumentException if a department with the same id already exists
     */
    void add(Department department);

    /**
     * Updates an existing department's name and location.
     *
     * @throws DepartmentNotFoundException if no department with the given id exists
     */
    void update(Department department) throws DepartmentNotFoundException;

    /**
     * Removes a department by id.
     *
     * @throws DepartmentNotFoundException if no department with the given id exists
     */
    void remove(int id) throws DepartmentNotFoundException;

    /** Returns the department with the given id, or empty if not found. */
    Optional<Department> findById(int id);

    /** Returns the department whose name matches exactly (case-insensitive), or empty. */
    Optional<Department> findByName(String name);

    /** Returns all departments ordered by id. */
    List<Department> findAll();

    /** Returns all departments in the given location (case-insensitive). */
    List<Department> findByLocation(String location);

    /** Returns the total number of departments. */
    int count();
}

