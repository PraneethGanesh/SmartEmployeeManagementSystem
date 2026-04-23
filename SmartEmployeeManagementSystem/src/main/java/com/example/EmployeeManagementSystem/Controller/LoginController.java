package com.example.EmployeeManagementSystem.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {

    @GetMapping("/login")
    public ResponseEntity<?> login() {
        return ResponseEntity.ok().body(Map.of("message", "Please use POST to /api/auth/login with credentials"));
    }
}
