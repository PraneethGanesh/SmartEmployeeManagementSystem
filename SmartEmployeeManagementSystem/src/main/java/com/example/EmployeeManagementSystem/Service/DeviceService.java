package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.AssignDeviceRequest;
import com.example.EmployeeManagementSystem.DTO.DeviceActivityDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceAssignmentResponseDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceResponseDTO;
import com.example.EmployeeManagementSystem.DTO.RepairLogResponseDTO;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RepairLog;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.AssignmentStatus;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.*;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final VendorRepo vendorRepo;
    private final DeviceAssignmentRepo deviceAssignmentRepo;
    private final EmployeeRepo employeeRepo;
    private final RepairLogRepository repairLogRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final RepairBillRepository repairBillRepository;
    private final VendorNegotiationRepository vendorNegotiationRepository;
    private final InvoiceRepository invoiceRepository;

    public DeviceService(DeviceRepository deviceRepository,
                         VendorRepo vendorRepo,
                         DeviceAssignmentRepo deviceAssignmentRepo,
                         EmployeeRepo employeeRepo,
                         RepairLogRepository repairLogRepository,
                         ServiceRequestRepository serviceRequestRepository,
                         RepairBillRepository repairBillRepository,
                         VendorNegotiationRepository vendorNegotiationRepository, InvoiceRepository invoiceRepository) {
        this.deviceRepository = deviceRepository;
        this.vendorRepo = vendorRepo;
        this.deviceAssignmentRepo = deviceAssignmentRepo;
        this.employeeRepo = employeeRepo;
        this.repairLogRepository = repairLogRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.repairBillRepository = repairBillRepository;
        this.vendorNegotiationRepository = vendorNegotiationRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public Device addDevice(DeviceDTO deviceDTO, Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Vendor vendor = vendorRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        Device device = new Device();
        device.setDeviceName(deviceDTO.getDeviceName());
        device.setWarrantyExpiryDate(deviceDTO.getWarrantyExpiryDate());
        device.setRentPerMonth(deviceDTO.getRentPerMonth());
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

        return deviceAssignmentRepo.findByAssignedToEmployeeIdAndStatus(employee.getEmployeeId(), AssignmentStatus.ACTIVE)
                .stream()
                .map(DeviceAssignment::getDevice)
                .filter(device -> device.getDeviceStatus() != DeviceStatus.CONDEMNED)
                .map(this::toDeviceResponse)
                .toList();
    }
    public List<DeviceResponseDTO> getDevicesForEmployee(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        return deviceAssignmentRepo.findByAssignedToEmployeeIdAndStatus(employee.getEmployeeId(), AssignmentStatus.ACTIVE)
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

    public List<DeviceResponseDTO> getAllDevices() {
        return deviceRepository.findByStatusNot(DeviceStatus.RETURNED_TO_VENDOR.name()).stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    public DeviceResponseDTO getDeviceDetails(Long deviceId, Authentication authentication) {
        Device device = getAuthorizedDevice(deviceId, authentication);
        return toDeviceResponse(device);
    }

    public List<DeviceActivityDTO> getDeviceActivity(Long deviceId, Authentication authentication) {
        Device device = getAuthorizedDevice(deviceId, authentication);
        List<DeviceActivityDTO> activities = new ArrayList<>();

        if (device.getCreatedAt() != null) {
            activities.add(activity(device.getCreatedAt(), "DEVICE_CREATED", "Device registered",
                    device.getBrand() + " " + device.getDeviceName() + " was added to inventory",
                    device.getTechVendor() != null ? device.getTechVendor().getName() : null,
                    device.getDeviceStatus() != null ? device.getDeviceStatus().name() : null));
        }

        deviceAssignmentRepo.findByDeviceIdOrderByAssignedDateDescIdDesc(deviceId).forEach(assignment -> {
            LocalDateTime occurredAt = assignment.getAssignedDate() != null
                    ? assignment.getAssignedDate().atStartOfDay()
                    : null;
            Employee assignedTo = assignment.getAssignedTo();
            activities.add(activity(occurredAt, "ASSIGNMENT", "Assigned to employee",
                    assignedTo != null ? "Assigned to " + assignedTo.getName() : "Device assignment updated",
                    assignedTo != null ? assignedTo.getName() : null,
                    assignment.getStatus() != null ? assignment.getStatus().name() : null));
        });

        serviceRequestRepository.findByDeviceOrderByRaisedAtDesc(device).forEach(request -> {
            activities.add(activity(request.getRaisedAt(), "SERVICE_REQUEST", "Service request raised",
                    request.getIssueDescription(),
                    request.getRaisedBy() != null ? request.getRaisedBy().getName() : null,
                    request.getStatus() != null ? request.getStatus().name() : null));

            if (request.getAdminRemarks() != null && !request.getAdminRemarks().isBlank()) {
                activities.add(activity(request.getRaisedAt(), "ADMIN_REVIEW", "Admin reviewed request",
                        request.getAdminRemarks(),
                        request.getReviewedBy() != null ? request.getReviewedBy().getName() : null,
                        request.getStatus() != null ? request.getStatus().name() : null));
            }

            if (request.getResolvedAt() != null) {
                activities.add(activity(request.getResolvedAt(), "RESOLUTION", "Request resolved",
                        request.getResolution(),
                        request.getDevice() != null && request.getDevice().getTechVendor() != null
                                ? request.getDevice().getTechVendor().getName()
                                : null,
                        request.getStatus() != null ? request.getStatus().name() : null));
            }
        });

        repairLogRepository.findByDeviceIdOrderByRepairDateDescIdDesc(deviceId).forEach(repairLog -> {
            LocalDateTime occurredAt = repairLog.getRepairDate() != null
                    ? repairLog.getRepairDate().atTime(LocalTime.NOON)
                    : null;
            activities.add(activity(occurredAt, "REPAIR_LOG", "Repair log updated",
                    repairLog.getRepairAction() != null ? repairLog.getRepairAction() : repairLog.getRemarks(),
                    repairLog.getRepairedBy(),
                    repairLog.getServiceRequest() != null && repairLog.getServiceRequest().getStatus() != null
                            ? repairLog.getServiceRequest().getStatus().name()
                            : null));
        });

        return activities.stream()
                .sorted(Comparator.comparing(DeviceActivityDTO::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
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
        assignment.setAssignedTo(employee);
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
        response.setWarrantyExpiryDate(device.getWarrantyExpiryDate());
        response.setRentPerMonth(device.getRentPerMonth());
        response.setDeviceType(device.getDeviceType());
        response.setDeviceStatus(device.getDeviceStatus());
        response.setCreatedAt(device.getCreatedAt());

        if (device.getTechVendor() != null) {
            response.setVendorName(device.getTechVendor().getName());
        }

        DeviceAssignment currentAssignment = device.getCurrentAssignment();
        if (currentAssignment != null) {
            response.setAssignedDate(currentAssignment.getAssignedDate());

            Employee assignedEmployee = currentAssignment.getAssignedTo();
            if (assignedEmployee != null) {
                response.setAssignedEmployeeId(assignedEmployee.getEmployeeId());
                response.setAssignedEmployeeName(assignedEmployee.getName());
            }
        }

        return response;
    }

    private Device getAuthorizedDevice(Long deviceId, Authentication authentication) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return device;
        }

        String email = AuthUtil.extractEmail(authentication);
        if (hasAuthority(authentication, "ROLE_TECH_VENDOR")) {
            if (device.getTechVendor() != null && device.getTechVendor().getEmail().equals(email)) {
                return device;
            }
            throw new AccessDeniedException("Unauthorized access to this device");
        }

        DeviceAssignment currentAssignment = device.getCurrentAssignment();
        if (currentAssignment != null
                && currentAssignment.getAssignedTo() != null
                && currentAssignment.getAssignedTo().getEmail().equals(email)) {
            return device;
        }

        throw new AccessDeniedException("Unauthorized access to this device");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private DeviceActivityDTO activity(LocalDateTime occurredAt,
                                       String type,
                                       String title,
                                       String description,
                                       String actorName,
                                       String status) {
        DeviceActivityDTO dto = new DeviceActivityDTO();
        dto.setOccurredAt(occurredAt);
        dto.setType(type);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setActorName(actorName);
        dto.setStatus(status);
        return dto;
    }

    public List<DeviceAssignmentResponseDTO> getDeviceAssignmentOfvendor(Authentication authentication) {
     String email=AuthUtil.extractEmail(authentication);
     Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
             ()->new VendorNotFoundException("Vendor:"+email+" not found")
     );
     List<DeviceAssignment> deviceAssignments=deviceAssignmentRepo.getAllAssignmentsByVendorId(vendor.getId());
     return deviceAssignments.stream().map(deviceAssignment -> toAssignmentDTO(deviceAssignment)).toList();
    }

    private DeviceAssignmentResponseDTO toAssignmentDTO(DeviceAssignment a) {
        DeviceAssignmentResponseDTO dto = new DeviceAssignmentResponseDTO();
        dto.setId(a.getId());
        dto.setStatus(a.getStatus());
        dto.setAssignedDate(a.getAssignedDate());

        if (a.getDevice() != null) {
            dto.setDeviceId(a.getDevice().getId());
            dto.setDeviceName(a.getDevice().getDeviceName());
            dto.setDeviceBrand(a.getDevice().getBrand());
            dto.setDeviceType(a.getDevice().getDeviceType());
        }

        if (a.getAssignedTo() != null) {
            dto.setEmployeeId(a.getAssignedTo().getEmployeeId());
            dto.setEmployeeName(a.getAssignedTo().getName()); // adjust to your Employee field
        }

        return dto;
    }

    public List<DeviceResponseDTO> getRepairedDevices() {
        return deviceRepository.findByDeviceStatus(DeviceStatus.REPAIR_DONE).stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    @Transactional
    public DeviceResponseDTO markDeviceAssignedAfterRepair(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceStatus() != DeviceStatus.REPAIR_DONE) {
            throw new IllegalArgumentException("Only REPAIR_DONE devices can be moved to ASSIGNED");
        }

        device.setDeviceStatus(DeviceStatus.ASSIGNED);
        Device savedDevice = deviceRepository.save(device);
        return toDeviceResponse(savedDevice);
    }

    public List<RepairLogResponseDTO> getRepairLogsForLoggedInVendorDevice(Long deviceId, Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Vendor vendor = vendorRepo.findByEmail(email).orElseThrow(
                () -> new VendorNotFoundException("Vendor:" + email + " not found")
        );

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getTechVendor() == null || !device.getTechVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Unauthorized access     to device repair logs");
        }

        return repairLogRepository.findByDeviceIdAndDeviceTechVendorIdOrderByRepairDateDescIdDesc(deviceId, vendor.getId()).stream()
                .map(this::toRepairLogResponseDTO)
                .toList();
    }

    private RepairLogResponseDTO toRepairLogResponseDTO(RepairLog repairLog) {
        RepairLogResponseDTO dto = new RepairLogResponseDTO();
        dto.setId(repairLog.getId());
        dto.setIssueType(repairLog.getIssueType());
        dto.setDamagedComponent(repairLog.getDamagedComponent());
        dto.setRepairAction(repairLog.getRepairAction());
        dto.setReplacedComponent(repairLog.getReplacedComponent());
        dto.setRepairedBy(repairLog.getRepairedBy());
        dto.setRepairDate(repairLog.getRepairDate());
        dto.setRemarks(repairLog.getRemarks());

        if (repairLog.getDevice() != null) {
            dto.setDeviceId(repairLog.getDevice().getId());
            dto.setDeviceName(repairLog.getDevice().getDeviceName());
        }

        if (repairLog.getServiceRequest() != null) {
            dto.setServiceRequestId(repairLog.getServiceRequest().getId());
            dto.setServiceRequestStatus(repairLog.getServiceRequest().getStatus() != null
                    ? repairLog.getServiceRequest().getStatus().name()
                    : null);
        }

        return dto;
    }

    @Transactional
    public ResponseEntity<String> returnCondemned(Long deviceId, Authentication authentication) {
        Device device = getAuthorizedDevice(deviceId, authentication);

        if (device.getDeviceStatus() != DeviceStatus.CONDEMNED) {
            throw new IllegalArgumentException("Only condemned devices can be returned");
        }

        DeviceAssignment currentAssignment = device.getCurrentAssignment();
        device.setCurrentAssignment(null);
        deviceAssignmentRepo.delete(currentAssignment);
        device.setDeviceStatus(DeviceStatus.RETURNED_TO_VENDOR);
        if (currentAssignment != null) {
            deviceAssignmentRepo.delete(currentAssignment);
        }
        deviceRepository.save(device);


        return ResponseEntity.ok("Condemned device returned and assignment removed");
    }

    @Transactional
    public ResponseEntity<String> removeDevice(Long deviceId, Authentication authentication) {

        Device device=deviceRepository.findById(deviceId).orElseThrow(
                ()-> new RuntimeException("Device Not found")
        );
        String email=AuthUtil.extractEmail(authentication);
        Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
           ()->new VendorNotFoundException("vendor  not found")
        );

        if (device.getTechVendor() == null || !vendor.getId().equals(device.getTechVendor().getId())) {
            throw new AccessDeniedException("Access Denied");
        }

        vendorNegotiationRepository.findByDeviceId(deviceId).forEach(negotiation -> negotiation.setDevice(null));
        repairBillRepository.deleteAll(repairBillRepository.findByDeviceId(deviceId));
        repairLogRepository.deleteAll(repairLogRepository.findByDeviceIdIn(List.of(deviceId)));
        serviceRequestRepository.deleteAll(serviceRequestRepository.findByDeviceId(deviceId));
        invoiceRepository.deleteAll(invoiceRepository.findByRepairBillDeviceId(deviceId));

        List<DeviceAssignment> deviceAssignments = deviceAssignmentRepo.findByDeviceIdOrderByAssignedDateDescIdDesc(deviceId);
        device.setCurrentAssignment(null);
        if (!deviceAssignments.isEmpty()) {
            deviceAssignmentRepo.deleteAll(deviceAssignments);
        }

        device.setTechVendor(null);
        deviceRepository.delete(device);
        return ResponseEntity.ok("Device removed from stock");

    }

    public List<DeviceResponseDTO> getDevicesUnderRepair(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
        Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("Vendor Not Found:"+email)
        );
        List<Device> devices=deviceRepository.findByTechVendorAndDeviceStatus(vendor,DeviceStatus.UNDER_REPAIR);
        return devices.stream().map(device -> toDeviceResponse(device)).toList();
    }
}
