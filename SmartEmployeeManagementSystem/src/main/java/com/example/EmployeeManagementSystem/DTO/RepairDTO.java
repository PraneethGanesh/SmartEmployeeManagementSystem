package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;

import java.math.BigDecimal;

public class RepairDTO {
    private Long requestId;
    private String resolution;
    private ServiceRequestStatus status;
    private String damagedComponent;
    private String replacedComponent;
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

    public String getDamagedComponent() {
        return damagedComponent;
    }

    public void setDamagedComponent(String damagedComponent) {
        this.damagedComponent = damagedComponent;
    }

    public String getReplacedComponent() {
        return replacedComponent;
    }

    public void setReplacedComponent(String replacedComponent) {
        this.replacedComponent = replacedComponent;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

}
