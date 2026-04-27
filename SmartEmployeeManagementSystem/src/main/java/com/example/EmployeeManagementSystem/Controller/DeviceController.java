package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AssignDeviceRequest;
import com.example.EmployeeManagementSystem.DTO.DeviceDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceResponseDTO;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<Device> addDevice(@RequestBody DeviceDTO deviceDTO,
                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.addDevice(deviceDTO, authentication));
    }

    @GetMapping("/vendor/me")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<List<DeviceResponseDTO>> getMyVendorDevices(Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDevicesForLoggedInVendor(authentication));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<List<DeviceResponseDTO>> getMyAssignedDevices(Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDevicesForLoggedInEmployee(authentication));
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceResponseDTO>> getUnassignedDevices() {
        return ResponseEntity.ok(deviceService.getUnassignedDevices());
    }

    @PostMapping("/{deviceId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignDevice(@PathVariable Long deviceId,
                                               @RequestBody AssignDeviceRequest request) {
        DeviceAssignment assignment = deviceService.assignDevice(deviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Device " + assignment.getDevice().getId()
                        + " assigned to employee " + assignment.getEmployee().getEmployeeId());
    }
}
