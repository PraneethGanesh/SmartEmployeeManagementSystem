package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.InvoiceStatus;
import com.example.EmployeeManagementSystem.Enum.InvoiceType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType; // REPAIR, RENTAL

    @OneToOne
    @JoinColumn(name = "repair_bill_id", unique = true)
    private RepairBill repairBill;   // null if rental

    @OneToOne
    @JoinColumn(name = "rental_bill_id", unique = true)
    private RentalBill rentalBill;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    private BigDecimal amount;

    private LocalDate issuedDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;    // SENT, SEEN, ACKNOWLEDGED

    private LocalDate seenAt;// when vendor marked it received

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RepairBill getRepairBill() {
        return repairBill;
    }

    public void setRepairBill(RepairBill repairBill) {
        this.repairBill = repairBill;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public RentalBill getRentalBill() {
        return rentalBill;
    }

    public void setRentalBill(RentalBill rentalBill) {
        this.rentalBill = rentalBill;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDate getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(LocalDate seenAt) {
        this.seenAt = seenAt;
    }
}
