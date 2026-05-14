package com.example.EmployeeManagementSystem;

import com.example.EmployeeManagementSystem.Entity.Employee;

public class EmployeeCreatedEvent {
  private final Employee employee;
  private final String password;

    public EmployeeCreatedEvent(Employee employee, String password) {
        this.employee = employee;
        this.password = password;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getPassword() {
        return password;
    }
}
