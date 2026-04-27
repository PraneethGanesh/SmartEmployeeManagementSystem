package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.DeviceType;

public class DeviceDTO {
    private String deviceName;       // e.g. "Dell XPS 15"
    private String brand;
    private DeviceType deviceType;

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

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
