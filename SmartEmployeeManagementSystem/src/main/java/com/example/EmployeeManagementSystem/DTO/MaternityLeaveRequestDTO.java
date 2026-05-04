package com.example.EmployeeManagementSystem.DTO;

import java.time.LocalDate;

public class MaternityLeaveRequestDTO {
    private LocalDate startDate;  // only start date — end is always fixed
    private String reason;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
