package com.example.EmployeeManagementSystem.Exception;

public class InvalidStartDateException extends RuntimeException{
    public InvalidStartDateException(String message) {
        super(message);
    }
}
