package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.*;
import com.example.EmployeeManagementSystem.Enum.*;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.*;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceRequestService {
    private final ServiceRequestRepository serviceRequestRepository;
    private final EmployeeRepo employeeRepo;
    private final DeviceRepository deviceRepository;
    private final RepairLogRepository repairLogRepository;
    private final VendorRepo vendorRepo;
    private final RepairBillRepository repairBillRepository;
    private final DeviceAssignmentRepo deviceAssignmentRepo;
    private final NotificationService notificationService;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository, EmployeeRepo employeeRepo, DeviceRepository deviceRepository, RepairLogRepository repairLogRepository, VendorRepo vendorRepo, RepairBillRepository repairBillRepository, DeviceAssignmentRepo deviceAssignmentRepo, NotificationService notificationService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.employeeRepo = employeeRepo;
        this.deviceRepository = deviceRepository;
        this.repairLogRepository = repairLogRepository;
        this.vendorRepo = vendorRepo;
        this.repairBillRepository = repairBillRepository;
        this.deviceAssignmentRepo = deviceAssignmentRepo;
        this.notificationService = notificationService;
    }

    public ServiceRequest createServiceRequest(ServiceRequestDTO dto, Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 2. Fetch device
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // 3. Validate device is assigned to employee
        if (device.getCurrentAssignment() == null ||
                !device.getCurrentAssignment().getAssignedTo().equals(employee)) {
            throw new RuntimeException("Device not assigned to this employee");
        }

        // 4. Validate device status
        if (device.getDeviceStatus() != DeviceStatus.ASSIGNED) {
            throw new RuntimeException("Device is not in usable state");
        }

        // 5. Create Service Request
        ServiceRequest request = new ServiceRequest();
        request.setDevice(device);
        request.setRaisedBy(employee);
        request.setRequestType(dto.getRequestType());
        request.setIssueDescription(dto.getIssueDescription());
        request.setUrgent(dto.isUrgent());
        request.setStatus(ServiceRequestStatus.OPEN);
        return serviceRequestRepository.save(request);
    }


    public List<ServiceRequestResponseDTO> getAllOpenServiceRequests(){
        List<ServiceRequest> serviceRequestList=serviceRequestRepository.findByStatus(ServiceRequestStatus.OPEN);
        return serviceRequestList.stream().map(serviceRequest -> toServiceRequestResponseDTO(serviceRequest)).toList();

    }

    public List<ServiceRequestResponseDTO> getMyServiceRequests(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFound("Employee not found: " + email));

        List<ServiceRequest> serviceRequestList = serviceRequestRepository.findByRaisedByAndStatusNotOrderByRaisedAtDesc(employee,ServiceRequestStatus.CLOSED);
        return serviceRequestList.stream()
                .map(this::toServiceRequestResponseDTO)
                .toList();
    }

    private ServiceRequestDTO toServiceRequestDTO(ServiceRequest serviceRequest){
        ServiceRequestDTO requestDTO=new ServiceRequestDTO();
        requestDTO.setDeviceId(serviceRequest.getDevice().getId());
        requestDTO.setRequestType(serviceRequest.getRequestType());
        requestDTO.setIssueDescription(serviceRequest.getIssueDescription());
        requestDTO.setUrgent(serviceRequest.isUrgent());
        return requestDTO;
    }

    private ServiceRequestResponseDTO toServiceRequestResponseDTO(ServiceRequest serviceRequest) {
        ServiceRequestResponseDTO responseDTO = new ServiceRequestResponseDTO();
        responseDTO.setId(serviceRequest.getId());
        responseDTO.setRequestType(serviceRequest.getRequestType() != null
                ? serviceRequest.getRequestType().name()
                : null);
        responseDTO.setStatus(serviceRequest.getStatus() != null
                ? serviceRequest.getStatus().name()
                : null);
        responseDTO.setIssueDescription(serviceRequest.getIssueDescription());
        responseDTO.setAdminRemarks(serviceRequest.getAdminRemarks());
        responseDTO.setResolution(serviceRequest.getResolution());
        responseDTO.setUrgent(serviceRequest.isUrgent());
        responseDTO.setRaisedAt(serviceRequest.getRaisedAt());
        responseDTO.setResolvedAt(serviceRequest.getResolvedAt());
        responseDTO.setDeviceName(serviceRequest.getDevice() != null
                ? serviceRequest.getDevice().getDeviceName()
                : null);
        responseDTO.setReviewedByName(serviceRequest.getReviewedBy() != null
                ? serviceRequest.getReviewedBy().getName()
                : null);
        responseDTO.setRaisedByName(serviceRequest.getRaisedBy().getName());
        return responseDTO;
    }


    @Transactional
    public ServiceRequestResponseDTO updateServiceRequestForRepairByAdmin(Authentication authentication, AdminActionDTO actionDTO){
        ServiceRequest serviceRequest=serviceRequestRepository.findById(actionDTO.getServiceRequestId()).orElseThrow(
                ()->new RuntimeException("Service Request Not Found")
        );
        String email=AuthUtil.extractEmail(authentication);
        Employee admin=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Admin not found:"+email)
        );
        Device device=serviceRequest.getDevice();
        switch (actionDTO.getStatus()){
            case SENT_FOR_REPAIR :
                device.setDeviceStatus(DeviceStatus.SENT_TO_VENDOR);
                deviceRepository.save(device);
                createRepairLogIfMissing(serviceRequest, actionDTO.getAdminRemarks());
                break;
            case REJECTED :
                device.setDeviceStatus(DeviceStatus.ASSIGNED);
                break;
        }
        serviceRequest.setReviewedBy(admin);
        serviceRequest.setAdminRemarks(actionDTO.getAdminRemarks());
        serviceRequest.setStatus(actionDTO.getStatus());
        ServiceRequest saved=serviceRequestRepository.save(serviceRequest);
        return toServiceRequestResponseDTO(saved);
    }

    public ServiceRequestResponseDTO updateServiceRequestForOtherServicesByAdmin(Authentication authentication, ApprovalActionDTO actionDTO) {
        ServiceRequest serviceRequest=serviceRequestRepository.findById(actionDTO.getServiceRequestId()).orElseThrow(
                ()->new RuntimeException("Service Request Not Found")
        );
        String email=AuthUtil.extractEmail(authentication);
        Employee admin=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Admin not found:"+email)
        );
        Device device=serviceRequest.getDevice();
        switch(actionDTO.getStatus()){
            case APPROVED :
                handelApproval(serviceRequest,device,actionDTO.getReplacementDeviceId());
                break;
            case REJECTED:
                device.setDeviceStatus(DeviceStatus.ASSIGNED);
                break;
        }
        deviceRepository.save(device);
        serviceRequest.setReviewedBy(admin);
        serviceRequest.setAdminRemarks(actionDTO.getAdminRemarks());
        serviceRequest.setStatus(ServiceRequestStatus.CLOSED);
        ServiceRequest saved=serviceRequestRepository.save(serviceRequest);
        return toServiceRequestResponseDTO(saved);
    }

    public void handelApproval(ServiceRequest serviceRequest, Device device,Long deviceId) {
        ServiceRequestType type=serviceRequest.getRequestType();
        DeviceAssignment current= device.getCurrentAssignment();
        if(type==ServiceRequestType.RETURN){
            device.setCurrentAssignment(null);
            device.setDeviceStatus(DeviceStatus.AVAILABLE);
            deviceAssignmentRepo.delete(current);
            System.out.println("deleted related assignment details for device:"+device.getDeviceName());
        }
        if(type==ServiceRequestType.REPLACEMENT){
            device.setCurrentAssignment(null);
            device.setDeviceStatus(DeviceStatus.AVAILABLE);
            deviceAssignmentRepo.delete(current);
            System.out.println("deleted related assignment details for device:"+device.getDeviceName());
            Device newDevice=deviceRepository.findById(deviceId).orElseThrow(
                    ()->new RuntimeException("Device not found:"+deviceId)
            );
            if(newDevice.getDeviceStatus()!=DeviceStatus.AVAILABLE){
                throw new RuntimeException("Device not available");
            }
            if(newDevice.getCurrentAssignment()!=null){
                throw new RuntimeException("Device already assigned");
            }
            Employee employee=employeeRepo.findByName(serviceRequest.getRaisedBy().getName()).orElseThrow(
                    ()->new EmployeeNotFound("Employee Not found:"+serviceRequest.getRaisedBy().getName())
            );
            DeviceAssignment deviceAssignment=new DeviceAssignment();
            deviceAssignment.setAssignedTo(employee);
            deviceAssignment.setAssignedDate(LocalDate.now());
            deviceAssignment.setDevice(newDevice);
            deviceAssignment.setStatus(AssignmentStatus.ACTIVE);
            DeviceAssignment saved=deviceAssignmentRepo.save(deviceAssignment);
            newDevice.setCurrentAssignment(saved);
            newDevice.setDeviceStatus(DeviceStatus.ASSIGNED);
            deviceRepository.save(newDevice);
        }

    }

    public List<ServiceRequestResponseDTO> getAllServiceRequestByVendor(Authentication authentication){
        String email=AuthUtil.extractEmail(authentication);
        Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("Vendor not found: "+email)
        );

        List<ServiceRequest> serviceRequestList=serviceRequestRepository.findByDeviceTechVendorAndStatus(vendor,
                ServiceRequestStatus.SENT_FOR_REPAIR);
        return serviceRequestList.stream()
                .map(serviceRequest -> toServiceRequestResponseDTO(serviceRequest))
                .toList();
    }

    public ResponseEntity<String> updateServiceRequestToUnderRepair(Authentication authentication, long serviceRequestId) {
        ServiceRequest serviceRequest=serviceRequestRepository.findById(serviceRequestId).orElseThrow(
                ()->new RuntimeException("Service Request Not Found")
        );
        Device device=serviceRequest.getDevice();
        serviceRequest.setStatus(ServiceRequestStatus.RECEIVED_BY_VENDOR);
        device.setDeviceStatus(DeviceStatus.UNDER_REPAIR);
        serviceRequestRepository.save(serviceRequest);
        deviceRepository.save(device);
        notificationService.notify(serviceRequest.getReviewedBy(),"Device:"+device.getDeviceName()+" is being repaired","DEVICE UNDER REPAIR");
        return ResponseEntity.ok(
                "Recieved the device and started the repair"
        );
    }

    @Transactional
    public ServiceRequestResponseDTO updateServiceRequestByVendor(Authentication authentication, RepairDTO repairDTO) {

        // 1. Get vendor
        String email = AuthUtil.extractEmail(authentication);
        Vendor vendor = vendorRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // 2. Get request
        ServiceRequest request = serviceRequestRepository.findById(repairDTO.getRequestId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // 3. Validate request belongs to vendor
        if (!request.getDevice().getTechVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        // 4. Validate status
        if (request.getStatus() != ServiceRequestStatus.RECEIVED_BY_VENDOR) {
            throw new RuntimeException("Request is not in repair stage");
        }

        Device device = request.getDevice();
        RepairLog repairLog = repairLogRepository.findByServiceRequest(request)
                .orElseThrow(() -> new RuntimeException("Repair log not found for request " + request.getId()));

        if (repairDTO.getStatus() == null) {
            throw new RuntimeException("Repair status is required");
        }

       RepairBill repairBill=new RepairBill();
        repairBill.setDevice(device);
        repairBill.setGeneratedDate(LocalDate.now());
        repairBill.setPaymentStatus(PaymentStatus.PENDING);

        // 5. Update based on repair result
        if (repairDTO.getStatus()==ServiceRequestStatus.REPAIR_DONE) {
            request.setResolution(repairDTO.getResolution());
            request.setStatus(repairDTO.getStatus());
            repairBill.setRepairCost(repairDTO.getRepairCost());
            device.setDeviceStatus(DeviceStatus.REPAIR_DONE);
            repairLog.setRemarks("Repair completed successfully");
        } else if(repairDTO.getStatus()==ServiceRequestStatus.IRREPARABLE){
            request.setResolution(repairDTO.getResolution());
            request.setStatus(repairDTO.getStatus());
            repairBill.setRepairCost(BigDecimal.ZERO);
            device.setDeviceStatus(DeviceStatus.CONDEMNED);
            repairLog.setRemarks("Repair closed without successful fix");
        }

        request.setResolvedAt(LocalDateTime.now());
        repairLog.setRepairAction(repairDTO.getResolution());
        repairLog.setRepairedBy(vendor.getName());
        repairLog.setRepairDate(LocalDate.now());
        if(repairDTO.getDamagedComponent()!=null){
            repairLog.setDamagedComponent(repairDTO.getDamagedComponent());
        }
        if(repairDTO.getReplacedComponent()!=null){
            repairLog.setReplacedComponent(repairDTO.getReplacedComponent());
        }

        RepairLog savedLog=repairLogRepository.save(repairLog);
        repairBill.setRepairLog(savedLog);
        repairBillRepository.save(repairBill);
        deviceRepository.save(device);
        request.setStatus(ServiceRequestStatus.CLOSED);
        ServiceRequest serviceRequest=serviceRequestRepository.save(request);
        return toServiceRequestResponseDTO(serviceRequest);
    }

    private void createRepairLogIfMissing(ServiceRequest serviceRequest, String adminRemarks) {
        if (repairLogRepository.findByServiceRequest(serviceRequest).isPresent()) {
            return;
        }

        RepairLog repairLog = new RepairLog();
        repairLog.setDevice(serviceRequest.getDevice());
        repairLog.setServiceRequest(serviceRequest);
        repairLog.setIssueType(serviceRequest.getRequestType() != null
                ? serviceRequest.getRequestType().name()
                : null);
        repairLog.setRemarks(adminRemarks);
        repairLogRepository.save(repairLog);
    }


    public List<ServiceRequestResponseDTO> getServiceRequestRecievedByVendor(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
        Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("Vendor Not found:"+email)
        );
        List<ServiceRequest> serviceRequestList=serviceRequestRepository.findByDeviceTechVendorAndStatus(vendor,ServiceRequestStatus.RECEIVED_BY_VENDOR);
        return serviceRequestList
                .stream()
                .map(serviceRequest -> toServiceRequestResponseDTO(serviceRequest))
                .toList();
    }
}


