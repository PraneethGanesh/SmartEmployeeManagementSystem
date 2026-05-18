package com.example.EmployeeManagementSystem.Repository;


import com.example.EmployeeManagementSystem.Entity.ServiceRequest;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;
import jdk.dynalink.linker.LinkerServices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest,Long> {

    List<ServiceRequest> findByStatus(ServiceRequestStatus status);
    List<ServiceRequest> findByRaisedByOrderByRaisedAtDesc(Employee employee);
    List<ServiceRequest> findByDeviceOrderByRaisedAtDesc(Device device);
    List<ServiceRequest> findByDeviceTechVendorAndStatus(
            Vendor vendor,
            ServiceRequestStatus status
    );

    List<ServiceRequest> findByRaisedByAndStatusNotOrderByRaisedAtDesc(
            Employee raisedBy,
            ServiceRequestStatus status
    );

    List<ServiceRequest> findByDeviceId(Long deviceId);

    List<ServiceRequest> findByRaisedBy(Employee employee);

    List<ServiceRequest> findByReviewedBy(Employee employee);
}
