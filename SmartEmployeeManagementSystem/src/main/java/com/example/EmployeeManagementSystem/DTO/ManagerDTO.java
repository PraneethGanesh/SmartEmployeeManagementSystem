package com.example.EmployeeManagementSystem.DTO;

import org.bouncycastle.crypto.signers.ISOTrailers;

public class ManagerDTO {
    private  long managerId;
    private String name;
    private String email;
    private String dept;

    public long getManagerId() {
        return managerId;
    }

    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }
}
