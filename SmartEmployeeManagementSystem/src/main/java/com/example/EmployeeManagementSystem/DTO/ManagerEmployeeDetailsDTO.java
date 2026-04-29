package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.Status;

import java.util.List;

public class ManagerEmployeeDetailsDTO {
    private Long employeeId;
    private String name;
    private String email;
    private String dept;
    private Status status;
    private String attendance;
    private List<DeviceResponseDTO> devices;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }

    public List<DeviceResponseDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceResponseDTO> devices) {
        this.devices = devices;
    }
}
