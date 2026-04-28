package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;
import com.example.EmployeeManagementSystem.Enum.ServiceRequestType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "raised_by")
    private Employee raisedBy;       // employee reporting issue

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;     // admin who reviewed

    @Enumerated(EnumType.STRING)
    private ServiceRequestType requestType;

    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus status;

    private String issueDescription; // "screen cracked", "won't boot"
    private String adminRemarks;     // admin notes on decision
    private String resolution;       // what was done finally

    private boolean urgent;          // employee can flag as urgent

    private LocalDateTime raisedAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() { this.raisedAt = LocalDateTime.now(); }

    public Employee getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(Employee raisedBy) {
        this.raisedBy = raisedBy;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Employee reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
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

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(LocalDateTime raisedAt) {
        this.raisedAt = raisedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
