package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveType;

import java.time.LocalDate;

public class LeaveResponseDTO {
    private long leaveRequestId;
    private long employeeId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private long numberOfDays;
    private String reason;
    private LeaveStatus status;

    public long getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(long leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
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
