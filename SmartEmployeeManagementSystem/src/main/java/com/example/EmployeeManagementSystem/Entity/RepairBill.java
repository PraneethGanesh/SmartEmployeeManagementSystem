package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class RepairBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private RepairLog repairLog;

    @ManyToOne
    private Device device;

    private BigDecimal repairCost;

    private LocalDate generatedDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    public RepairLog getRepairLog() {
        return repairLog;
    }

    public void setRepairLog(RepairLog repairLog) {
        this.repairLog = repairLog;
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

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
