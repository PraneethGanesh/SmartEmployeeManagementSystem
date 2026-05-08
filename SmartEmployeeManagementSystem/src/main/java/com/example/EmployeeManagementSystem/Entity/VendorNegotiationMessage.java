package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.MessageSenderType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_negotiation_message")
public class VendorNegotiationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negotiation_id", nullable = false)
    private VendorNegotiation negotiation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSenderType senderType;

    @Column(nullable = false)
    private String senderEmail;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VendorNegotiation getNegotiation() { return negotiation; }
    public void setNegotiation(VendorNegotiation negotiation) { this.negotiation = negotiation; }

    public MessageSenderType getSenderType() { return senderType; }
    public void setSenderType(MessageSenderType senderType) { this.senderType = senderType; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
