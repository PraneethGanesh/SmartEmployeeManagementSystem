package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MessageSenderType;

import java.time.LocalDateTime;

public class VendorNegotiationMessageResponse {
    private Long id;
    private MessageSenderType senderType;
    private String senderEmail;
    private String message;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MessageSenderType getSenderType() { return senderType; }
    public void setSenderType(MessageSenderType senderType) { this.senderType = senderType; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
