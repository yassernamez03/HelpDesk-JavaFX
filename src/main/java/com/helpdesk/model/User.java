package com.helpdesk.model;

public class User {
    private int id;
    private String username;
    private String department;

    public User(int id, String username, String department) {
        this.id = id;
        this.username = username;
        this.department = department;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return username + " (" + department + ")";
    }
}