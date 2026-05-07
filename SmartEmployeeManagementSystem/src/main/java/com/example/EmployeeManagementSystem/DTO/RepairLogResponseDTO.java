package com.example.EmployeeManagementSystem.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepairLogResponseDTO {
    private Long id;
    private Long deviceId;
    private String deviceName;
    private Long serviceRequestId;
    private String serviceRequestStatus;
    private String issueType;
    private String damagedComponent;
    private String repairAction;
    private String replacedComponent;
    private BigDecimal repairCost;
    private String repairedBy;
    private LocalDate repairDate;
    private String remarks;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getServiceRequestStatus() {
        return serviceRequestStatus;
    }

    public void setServiceRequestStatus(String serviceRequestStatus) {
        this.serviceRequestStatus = serviceRequestStatus;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getDamagedComponent() {
        return damagedComponent;
    }

    public void setDamagedComponent(String damagedComponent) {
        this.damagedComponent = damagedComponent;
    }

    public String getRepairAction() {
        return repairAction;
    }

    public void setRepairAction(String repairAction) {
        this.repairAction = repairAction;
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

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public String getRepairedBy() {
        return repairedBy;
    }

    public void setRepairedBy(String repairedBy) {
        this.repairedBy = repairedBy;
    }

    public LocalDate getRepairDate() {
        return repairDate;
    }

    public void setRepairDate(LocalDate repairDate) {
        this.repairDate = repairDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
