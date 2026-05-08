package com.example.EmployeeManagementSystem.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repair_log")
public class RepairLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @OneToOne
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    private ServiceRequest serviceRequest;

    private String issueType;

    private String damagedComponent;

    private String repairAction;

    private String replacedComponent;

    private String repairedBy;

    private LocalDate repairDate;

    private String remarks;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
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
