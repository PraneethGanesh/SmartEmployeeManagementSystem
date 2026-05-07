package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AssignDeviceRequest;
import com.example.EmployeeManagementSystem.DTO.DeviceActivityDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceAssignmentResponseDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceDTO;
import com.example.EmployeeManagementSystem.DTO.DeviceResponseDTO;
import com.example.EmployeeManagementSystem.DTO.RepairLogResponseDTO;
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceResponseDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','TECH_VENDOR','EMPLOYEE','MANAGER')")
    public ResponseEntity<DeviceResponseDTO> getDeviceDetails(@PathVariable Long deviceId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDeviceDetails(deviceId, authentication));
    }

    @GetMapping("/{deviceId}/activity")
    @PreAuthorize("hasAnyRole('ADMIN','TECH_VENDOR','EMPLOYEE','MANAGER')")
    public ResponseEntity<List<DeviceActivityDTO>> getDeviceActivity(@PathVariable Long deviceId,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(deviceService.getDeviceActivity(deviceId, authentication));
    }

    @PostMapping("/{deviceId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignDevice(@PathVariable Long deviceId,
                                               @RequestBody AssignDeviceRequest request) {
        DeviceAssignment assignment = deviceService.assignDevice(deviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Device " + assignment.getDevice().getId()
                        + " assigned to employee " + assignment.getAssignedTo().getEmployeeId());
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public List<DeviceAssignmentResponseDTO> getDeviceAssignmentOfvendor(Authentication authentication){
        return deviceService.getDeviceAssignmentOfvendor(authentication);

    }

    @GetMapping("/repaired_devices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DeviceResponseDTO> getRepairedDevices(){
      return deviceService.getRepairedDevices();
    }

    @PutMapping("/{deviceId}/mark-assigned")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceResponseDTO markDeviceAssignedAfterRepair(@PathVariable Long deviceId) {
        return deviceService.markDeviceAssignedAfterRepair(deviceId);
    }

    @GetMapping("/vendor/{deviceId}/repair-logs")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public List<RepairLogResponseDTO> getVendorRepairLogs(@PathVariable Long deviceId, Authentication authentication) {
        return deviceService.getRepairLogsForLoggedInVendorDevice(deviceId, authentication);
    }

}
