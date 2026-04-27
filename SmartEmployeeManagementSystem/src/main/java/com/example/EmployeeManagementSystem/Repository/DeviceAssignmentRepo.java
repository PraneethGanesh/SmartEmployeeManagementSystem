package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Enum.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceAssignmentRepo extends JpaRepository<DeviceAssignment,Long> {
    Optional<DeviceAssignment> findByDeviceIdAndStatus(Long deviceId, AssignmentStatus status);
    java.util.List<DeviceAssignment> findByEmployeeEmployeeIdAndStatus(Long employeeId, AssignmentStatus status);
}
