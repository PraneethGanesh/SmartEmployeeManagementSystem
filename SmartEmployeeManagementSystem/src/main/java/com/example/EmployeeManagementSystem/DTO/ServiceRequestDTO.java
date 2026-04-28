package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.ServiceRequestType;

public class ServiceRequestDTO {

    private Long deviceId;
    private ServiceRequestType requestType;
    private String issueDescription;
    private boolean urgent;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public ServiceRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(ServiceRequestType requestType) {
        this.requestType = requestType;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }
}
