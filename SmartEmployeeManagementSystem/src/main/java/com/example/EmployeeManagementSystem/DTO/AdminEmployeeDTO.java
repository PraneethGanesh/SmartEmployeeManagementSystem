package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.Gender;

public class AdminEmployeeDTO {
    private String name;
    private String email;
    private String password;
    private Gender gender;
    private String timezone;
    private long managerId;

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getTimezone() {
        return timezone;
    }

    public Gender getGender() {
        return gender;
    }

    public long getManagerId() {
        return managerId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setName(String name) {
        this.name = name;
    }
}
