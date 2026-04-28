package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.AdminActionDTO;
import com.example.EmployeeManagementSystem.DTO.RepairDTO;
import com.example.EmployeeManagementSystem.DTO.ServiceRequestDTO;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.ServiceRequest;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import com.example.EmployeeManagementSystem.Enum.ServiceRequestStatus;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.DeviceRepository;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.ServiceRequestRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

@Service
public class ServiceRequestService {
    private final ServiceRequestRepository serviceRequestRepository;
    private final EmployeeRepo employeeRepo;
    private final DeviceRepository deviceRepository;
    private final VendorRepo vendorRepo;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository, EmployeeRepo employeeRepo, DeviceRepository deviceRepository, VendorRepo vendorRepo) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.employeeRepo = employeeRepo;
        this.deviceRepository = deviceRepository;
        this.vendorRepo = vendorRepo;
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


    public List<ServiceRequestDTO> getAllOpenServiceRequests(){
        List<ServiceRequest> serviceRequestList=serviceRequestRepository.findByStatus(ServiceRequestStatus.OPEN);
        return serviceRequestList.stream().map(serviceRequest -> toServiceRequestDTO(serviceRequest)).toList();

    }

    private ServiceRequestDTO toServiceRequestDTO(ServiceRequest serviceRequest){
        ServiceRequestDTO requestDTO=new ServiceRequestDTO();
        requestDTO.setDeviceId(serviceRequest.getDevice().getId());
        requestDTO.setRequestType(serviceRequest.getRequestType());
        requestDTO.setIssueDescription(serviceRequest.getIssueDescription());
        requestDTO.setUrgent(serviceRequest.isUrgent());
        return requestDTO;
    }

    public ServiceRequest updateServiceRequestByAdmin(Authentication authentication, AdminActionDTO actionDTO){
        ServiceRequest serviceRequest=serviceRequestRepository.findById(actionDTO.getServiceRequestId()).orElseThrow(
                ()->new RuntimeException("Service Request Not Found")
        );
        String email=AuthUtil.extractEmail(authentication);
        Employee admin=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Admin not found:"+email)
        );
        Device device=serviceRequest.getDevice();
        if(actionDTO.getStatus()==ServiceRequestStatus.SENT_FOR_REPAIR){
            device.setDeviceStatus(DeviceStatus.UNDER_REPAIR);
            deviceRepository.save(device);
        }
        serviceRequest.setReviewedBy(admin);
        serviceRequest.setAdminRemarks(actionDTO.getAdminRemarks());
        serviceRequest.setStatus(actionDTO.getStatus());
        return serviceRequestRepository.save(serviceRequest);
    }

    public List<ServiceRequest> getAllServiceRequestByVendor(Authentication authentication){
        String email=AuthUtil.extractEmail(authentication);
        Vendor vendor=vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("Vendor not found: "+email)
        );

        return serviceRequestRepository.findByDeviceTechVendorAndStatus(vendor,ServiceRequestStatus.SENT_FOR_REPAIR);
    }

    public ServiceRequest updateServiceRequestByVendor(
            Authentication authentication, RepairDTO repairDTO
            ) {

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
        if (request.getStatus() != ServiceRequestStatus.SENT_FOR_REPAIR) {
            throw new RuntimeException("Request is not in repair stage");
        }

        Device device = request.getDevice();

        // 5. Update based on repair result
        if (repairDTO.getStatus()==ServiceRequestStatus.REPAIR_DONE) {
            request.setResolution(repairDTO.getResolution());
            request.setStatus(repairDTO.getStatus());

            device.setDeviceStatus(DeviceStatus.ASSIGNED); // back to employee
        } else {
            request.setResolution(repairDTO.getResolution());
            request.setStatus(ServiceRequestStatus.REPAIR_DONE);

            device.setDeviceStatus(DeviceStatus.CONDEMNED); // not usable
        }

        request.setResolvedAt(LocalDateTime.now());

        deviceRepository.save(device);
        return serviceRequestRepository.save(request);
    }
}


