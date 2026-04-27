package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device,Long> {
    List<Device> findByTechVendorId(Long vendorId);
    List<Device> findByDeviceStatus(DeviceStatus deviceStatus);
}
