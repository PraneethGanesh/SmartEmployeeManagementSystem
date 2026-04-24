package com.example.EmployeeManagementSystem.DTO;

import java.util.List;

public class EmployeeAttendanceResponseDTO {
    private long workingCount;
    private long onLeaveCount;
    private List<EmployeeStatusDTO> workingEmployees;
    private List<EmployeeStatusDTO> onLeaveEmployees;

    public long getWorkingCount() {
        return workingCount;
    }

    public void setWorkingCount(long workingCount) {
        this.workingCount = workingCount;
    }

    public long getOnLeaveCount() {
        return onLeaveCount;
    }

    public void setOnLeaveCount(long onLeaveCount) {
        this.onLeaveCount = onLeaveCount;
    }

    public List<EmployeeStatusDTO> getWorkingEmployees() {
        return workingEmployees;
    }

    public void setWorkingEmployees(List<EmployeeStatusDTO> workingEmployees) {
        this.workingEmployees = workingEmployees;
    }

    public List<EmployeeStatusDTO> getOnLeaveEmployees() {
        return onLeaveEmployees;
    }

    public void setOnLeaveEmployees(List<EmployeeStatusDTO> onLeaveEmployees) {
        this.onLeaveEmployees = onLeaveEmployees;
    }
}
