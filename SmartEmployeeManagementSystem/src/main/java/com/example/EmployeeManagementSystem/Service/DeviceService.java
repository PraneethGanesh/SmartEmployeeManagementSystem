package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.AssignDeviceRequest;
import com.example.EmployeeManagementSystem.DTO.DeviceDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceResponseDTO;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.AssignmentStatus;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.DeviceAssignmentRepo;
import com.example.EmployeeManagementSystem.Repository.DeviceRepository;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final VendorRepo vendorRepo;
    private final DeviceAssignmentRepo deviceAssignmentRepo;
    private final EmployeeRepo employeeRepo;

    public DeviceService(DeviceRepository deviceRepository,
                         VendorRepo vendorRepo,
                         DeviceAssignmentRepo deviceAssignmentRepo,
                         EmployeeRepo employeeRepo) {
        this.deviceRepository = deviceRepository;
        this.vendorRepo = vendorRepo;
        this.deviceAssignmentRepo = deviceAssignmentRepo;
        this.employeeRepo = employeeRepo;
    }

    public Device addDevice(DeviceDTO deviceDTO, Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Vendor vendor = vendorRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        Device device = new Device();
        device.setDeviceName(deviceDTO.getDeviceName());
        device.setBrand(deviceDTO.getBrand());
        device.setDeviceStatus(DeviceStatus.AVAILABLE);
        device.setDeviceType(deviceDTO.getDeviceType());
        device.setTechVendor(vendor);
        device.setCreatedAt(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    public List<DeviceResponseDTO> getDevicesForLoggedInVendor(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Vendor vendor = vendorRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        return deviceRepository.findByTechVendorId(vendor.getId()).stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    public List<DeviceResponseDTO> getDevicesForLoggedInEmployee(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        return deviceAssignmentRepo.findByEmployeeEmployeeIdAndStatus(employee.getEmployeeId(), AssignmentStatus.ACTIVE)
                .stream()
                .map(DeviceAssignment::getDevice)
                .map(this::toDeviceResponse)
                .toList();
    }

    public List<DeviceResponseDTO> getUnassignedDevices() {
        return deviceRepository.findByDeviceStatus(DeviceStatus.AVAILABLE).stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    @Transactional
    public DeviceAssignment assignDevice(Long deviceId, AssignDeviceRequest request) {
        if (request.getEmployeeId() == null) {
            throw new IllegalArgumentException("employeeId is required");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        Employee employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (employee.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Device can only be assigned to an active user");
        }

        if (!Set.of(Role.EMPLOYEE, Role.MANAGER, Role.ADMIN).contains(employee.getRole())) {
            throw new IllegalArgumentException("Devices can only be assigned to employees, managers, or admins");
        }

        if (device.getDeviceStatus() != DeviceStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available devices can be assigned");
        }

        deviceAssignmentRepo.findByDeviceIdAndStatus(deviceId, AssignmentStatus.ACTIVE)
                .ifPresent(existingAssignment -> {
                    throw new IllegalArgumentException("Device is already assigned");
                });

        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDevice(device);
        assignment.setEmployee(employee);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setAssignedDate(LocalDate.now());

        DeviceAssignment savedAssignment = deviceAssignmentRepo.save(assignment);
        device.setCurrentAssignment(savedAssignment);
        device.setDeviceStatus(DeviceStatus.ASSIGNED);
        deviceRepository.save(device);

        return savedAssignment;
    }

    private DeviceResponseDTO toDeviceResponse(Device device) {
        DeviceResponseDTO response = new DeviceResponseDTO();
        response.setId(device.getId());
        response.setDeviceName(device.getDeviceName());
        response.setBrand(device.getBrand());
        response.setDeviceType(device.getDeviceType());
        response.setDeviceStatus(device.getDeviceStatus());

        if (device.getTechVendor() != null) {
            response.setVendorId(device.getTechVendor().getId());
        }

        DeviceAssignment currentAssignment = device.getCurrentAssignment();
        if (currentAssignment != null) {
            response.setAssignedDate(currentAssignment.getAssignedDate());

            Employee assignedEmployee = currentAssignment.getEmployee();
            if (assignedEmployee != null) {
                response.setAssignedEmployeeId(assignedEmployee.getEmployeeId());
                response.setAssignedEmployeeName(assignedEmployee.getName());
            }
        }

        return response;
    }
}
