package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;

import java.math.BigDecimal;

public class RepairDTO {
    private Long requestId;
    private String resolution;
    private ServiceRequestStatus status;
    private BigDecimal repairCost;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }
}
