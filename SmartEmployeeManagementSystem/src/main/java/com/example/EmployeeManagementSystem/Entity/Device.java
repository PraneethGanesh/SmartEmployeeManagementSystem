package com.example.EmployeeManagementSystem.Entity;


import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import com.example.EmployeeManagementSystem.Enum.DeviceType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String deviceName;       // e.g. "Dell XPS 15"
    private String brand;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor techVendor;

    @OneToOne(mappedBy = "device")
    private DeviceAssignment currentAssignment;

    private DeviceStatus deviceStatus;

    private DeviceType deviceType;

    private LocalDateTime createdAt;

    private LocalDate warrantyExpiryDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Vendor getTechVendor() {
        return techVendor;
    }

    public void setTechVendor(Vendor techVendor) {
        this.techVendor = techVendor;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public DeviceAssignment getCurrentAssignment() {
        return currentAssignment;
    }

    public void setCurrentAssignment(DeviceAssignment currentAssignment) {
        this.currentAssignment = currentAssignment;
    }

    public LocalDate getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) {
        this.warrantyExpiryDate = warrantyExpiryDate;
    }
}
