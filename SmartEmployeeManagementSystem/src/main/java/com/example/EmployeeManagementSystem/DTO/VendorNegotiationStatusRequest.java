package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.NegotiationStatus;

public class VendorNegotiationStatusRequest {
    private NegotiationStatus status;

    public NegotiationStatus getStatus() { return status; }
    public void setStatus(NegotiationStatus status) { this.status = status; }
}
