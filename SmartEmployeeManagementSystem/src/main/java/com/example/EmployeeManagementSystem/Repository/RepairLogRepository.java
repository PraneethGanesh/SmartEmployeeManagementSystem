package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.RepairLog;
import com.example.EmployeeManagementSystem.Entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepairLogRepository extends JpaRepository<RepairLog, Long> {
    Optional<RepairLog> findByServiceRequest(ServiceRequest serviceRequest);
    List<RepairLog> findByDeviceIdIn(List<Long> deviceIds);
    List<RepairLog> findByDeviceIdOrderByRepairDateDescIdDesc(Long deviceId);
    List<RepairLog> findByDeviceIdAndDeviceTechVendorIdOrderByRepairDateDescIdDesc(Long deviceId, Long vendorId);
    List<RepairLog> findByDeviceIdAndRepairDateBetweenOrderByRepairDateDesc(Long deviceId,
                                                                            LocalDate start,
                                                                            LocalDate end);
}
