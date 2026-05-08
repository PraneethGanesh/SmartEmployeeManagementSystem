package com.example.EmployeeManagementSystem.DTO;


import java.math.BigDecimal;
import java.time.LocalDate;

public class RepairBillDTO {
    private Long billId;
    private String deviceName;
    private String repairedBy;
    private LocalDate repairedDate;
    private BigDecimal repairCost;
    private LocalDate geneartedOn;
    private String status;

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getRepairedBy() {
        return repairedBy;
    }

    public void setRepairedBy(String repairedBy) {
        this.repairedBy = repairedBy;
    }

    public LocalDate getRepairedDate() {
        return repairedDate;
    }

    public void setRepairedDate(LocalDate repairedDate) {
        this.repairedDate = repairedDate;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public LocalDate getGeneartedOn() {
        return geneartedOn;
    }

    public void setGeneartedOn(LocalDate geneartedOn) {
        this.geneartedOn = geneartedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
