package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.NegotiationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VendorNegotiationResponse {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private Long deviceId;
    private String deviceName;
    private String subject;
    private String description;
    private BigDecimal proposedAmount;
    private NegotiationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<VendorNegotiationMessageResponse> messages;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getProposedAmount() { return proposedAmount; }
    public void setProposedAmount(BigDecimal proposedAmount) { this.proposedAmount = proposedAmount; }

    public NegotiationStatus getStatus() { return status; }
    public void setStatus(NegotiationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<VendorNegotiationMessageResponse> getMessages() { return messages; }
    public void setMessages(List<VendorNegotiationMessageResponse> messages) { this.messages = messages; }
}
