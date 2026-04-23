package com.example.EmployeeManagementSystem.Exception;

public class LeaveRequestNotFoundException extends RuntimeException{
    public LeaveRequestNotFoundException(String message) {
        super(message);
    }
}
