package com.example.EmployeeManagementSystem.Exception;

public class InvalidEndDateException extends RuntimeException{
    public InvalidEndDateException(String message) {
        super(message);
    }
}
