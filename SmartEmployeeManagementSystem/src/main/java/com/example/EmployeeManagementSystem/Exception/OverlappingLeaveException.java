package com.example.EmployeeManagementSystem.Exception;

public class OverlappingLeaveException extends RuntimeException{
    public OverlappingLeaveException(String message) {
        super(message);
    }
}
