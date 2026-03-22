package com.example.helloworld.exception;

/**
 * Thrown when a Department lookup by id or name finds no match.
 */
public class DepartmentNotFoundException extends Exception {

    private final String searchKey;

    public DepartmentNotFoundException(int id) {
        super("No department found with id: " + id);
        this.searchKey = String.valueOf(id);
    }

    public DepartmentNotFoundException(String name) {
        super("No department found with name: " + name);
        this.searchKey = name;
    }

    public String getSearchKey() {
        return searchKey;
    }
}

