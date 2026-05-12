package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;


public class ApprovalActionDTO {
    private Long serviceRequestId;
    private ServiceRequestStatus status;  // APPROVED or REJECTED
    private String adminRemarks;
    private Long replacementDeviceId;     // which device to assign (for REPLACEMENT/UPGRADE)

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public Long getReplacementDeviceId() {
        return replacementDeviceId;
    }

    public void setReplacementDeviceId(Long replacementDeviceId) {
        this.replacementDeviceId = replacementDeviceId;
    }
}
