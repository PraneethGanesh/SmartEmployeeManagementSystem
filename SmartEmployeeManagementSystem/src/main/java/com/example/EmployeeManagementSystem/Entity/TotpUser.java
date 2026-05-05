package com.example.EmployeeManagementSystem.Entity;

public interface TotpUser {
    String getEmail();
    String getName();
    String getTotpSecret();
    void setTotpSecret(String secret);
    boolean isTotpEnabled();
    void setTotpEnabled(boolean enabled);
}