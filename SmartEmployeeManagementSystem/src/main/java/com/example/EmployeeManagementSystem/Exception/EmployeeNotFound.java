package com.example.EmployeeManagementSystem.Exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class EmployeeNotFound extends UsernameNotFoundException {
    public EmployeeNotFound(String message) {
        super(message);
    }
}
