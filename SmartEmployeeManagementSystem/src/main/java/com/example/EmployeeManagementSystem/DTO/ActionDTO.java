package com.example.EmployeeManagementSystem.DTO;


public class ActionDTO {
    private long leaveRequestId;
    private String action;
    private String remarks;

    public long getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(long leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }


    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
