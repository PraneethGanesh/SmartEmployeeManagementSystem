package com.example.EmployeeManagementSystem.DTO;


import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthRequest {
    @JsonProperty("username")
    private String Username;
    @JsonProperty("password")
    private String Password;

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }
}
