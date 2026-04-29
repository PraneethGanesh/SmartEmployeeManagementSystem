package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveType;

import java.time.LocalDate;

public class ManagerLeaveResponseDTO {
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private long numberOfDays;
    private String reason;
    private LeaveStatus status;

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(long numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }
}
