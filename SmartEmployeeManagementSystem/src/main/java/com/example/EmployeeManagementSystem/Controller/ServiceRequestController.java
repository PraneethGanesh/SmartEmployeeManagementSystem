package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.ServiceRequest;
import com.example.EmployeeManagementSystem.Service.ServiceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-requests")
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<ServiceRequest> createServiceRequest(
            @RequestBody ServiceRequestDTO serviceRequestDTO,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRequestService.createServiceRequest(serviceRequestDTO, authentication));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<List<ServiceRequestResponseDTO>> getMyServiceRequests(Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.getMyServiceRequests(authentication));
    }

    @GetMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceRequestResponseDTO>> getAllOpenServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.getAllOpenServiceRequests());
    }

    @PutMapping("/admin/repair")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceRequestResponseDTO> updateServiceRequestForRepair(
            Authentication authentication,
            @RequestBody AdminActionDTO actionDTO) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestForRepairByAdmin(authentication, actionDTO));
    }

    @PutMapping("/admin/other")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceRequestResponseDTO> updateServiceRequestForOtherServices(
            Authentication authentication,
            @RequestBody ApprovalActionDTO actionDTO) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestForOtherServicesByAdmin(authentication, actionDTO));
    }


    @GetMapping("/vendor/me")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<List<ServiceRequestResponseDTO>> getAllServiceRequestByVendor(Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.getAllServiceRequestByVendor(authentication));
    }

    @PutMapping("/vendor/finalUpdate")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<ServiceRequestResponseDTO> updateServiceRequestByVendor(
            Authentication authentication,
            @RequestBody RepairDTO repairDTO) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestByVendor(authentication, repairDTO));
    }

    @PutMapping("/vendor/{serviceRequestId}")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<String> updateServiceRequest(Authentication authentication,
                                                       @PathVariable long serviceRequestId
    ){
        return serviceRequestService.updateServiceRequestToUnderRepair(authentication,serviceRequestId);
    }

    @GetMapping("/devices/repair")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<List<ServiceRequestResponseDTO>> getServiceRequestRecievedByVendor(Authentication authentication){
        return ResponseEntity.ok(serviceRequestService.getServiceRequestRecievedByVendor(authentication));
    }
}
