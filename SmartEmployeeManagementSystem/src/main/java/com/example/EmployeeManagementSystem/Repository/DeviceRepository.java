package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device,Long> {
    List<Device> findByTechVendorId(Long vendorId);
    List<Device> findByDeviceStatus(DeviceStatus deviceStatus);
    @Query(value = "select * from device d where d.device_status != :deviceStatus",nativeQuery = true)
    List<Device> findByStatusNot(@Param("deviceStatus") String deviceStatus);

    @Query(value = "select * from device d where d.vendor_id=:vendorId AND d.device_status IN (:status)",nativeQuery = true)
    List<Device> findByStatus(@Param("vendorId") long vendorId,@Param("status") List<String> status);

    List<Device> findByTechVendorAndDeviceStatus(Vendor vendor,DeviceStatus deviceStatus);

}
