package com.example.EmployeeManagementSystem.DTO;

public class AccessTokenRequest {
    private String refreshToken;

    public AccessTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
