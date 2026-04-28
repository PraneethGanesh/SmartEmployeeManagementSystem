package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Enum.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceAssignmentRepo extends JpaRepository<DeviceAssignment,Long> {
    Optional<DeviceAssignment> findByDeviceIdAndStatus(Long deviceId, AssignmentStatus status);
    List<DeviceAssignment> findByAssignedToEmployeeIdAndStatus(Long employeeId, AssignmentStatus status);

    @Query(value = "SELECT da.* FROM device_assignment da JOIN device d ON da.device_id = d.id WHERE d.vendor_id = :vendorId",nativeQuery = true)
    List<DeviceAssignment> getAllAssignmentsByVendorId(@Param("vendorId") long vendorId);


}
