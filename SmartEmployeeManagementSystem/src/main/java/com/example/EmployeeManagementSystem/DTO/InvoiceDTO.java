package com.example.EmployeeManagementSystem.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceDTO {

    private Long id;
    private String invoiceType;
    private String vendorName;

    private BigDecimal amount;

    private LocalDate issuedDate;
    private String status;    // SENT, SEEN, ACKNOWLEDGED

    private LocalDate seenAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(LocalDate seenAt) {
        this.seenAt = seenAt;
    }
}
