package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AdminActionDTO;
import com.example.EmployeeManagementSystem.DTO.RepairDTO;
import com.example.EmployeeManagementSystem.DTO.ServiceRequestDTO;
import com.example.EmployeeManagementSystem.Entity.ServiceRequest;
import com.example.EmployeeManagementSystem.Service.ServiceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceRequestDTO>> getAllOpenServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.getAllOpenServiceRequests());
    }

    @PutMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceRequest> updateServiceRequestByAdmin(
            Authentication authentication,
            @RequestBody AdminActionDTO actionDTO) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestByAdmin(authentication, actionDTO));
    }

    @GetMapping("/vendor/me")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<List<ServiceRequest>> getAllServiceRequestByVendor(Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.getAllServiceRequestByVendor(authentication));
    }

    @PutMapping("/vendor")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<ServiceRequest> updateServiceRequestByVendor(
            Authentication authentication,
            @RequestBody RepairDTO repairDTO) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestByVendor(authentication, repairDTO));
    }
}
