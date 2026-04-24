package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.ActionDTO;
import com.example.EmployeeManagementSystem.DTO.LeaveRequestDTO;
import com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO;
import com.example.EmployeeManagementSystem.Service.LeaveRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("leave_requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<LeaveResponseDTO>> getAllLeaveRequest() {
        return ResponseEntity.ok(leaveRequestService.getAllTheLeaveRequest());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<?> addLeaveRequest(Authentication authentication,@RequestBody LeaveRequestDTO dto){
        return leaveRequestService.createRequest(authentication,dto);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<LeaveResponseDTO>> getAllTheLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.getAllThePendingLeaveRequests());
    }

    @PutMapping("/approval")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateLeaveRequestStatus(@RequestBody ActionDTO actionDTO,Authentication authentication){
      return leaveRequestService.updateLeaveRequestStatus(actionDTO,authentication);
    }

    @PutMapping("/cancel/{leaveId}")
    @PreAuthorize("hasAuthority('LEAVE_CANCEL')")
    public ResponseEntity<?> cancelLeaveRequest(Authentication authentication,@PathVariable long leaveId){
      return leaveRequestService.cancelLeaveRequest(authentication, leaveId);
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<?> getEmployeeLeaves(Authentication authentication) {
        return leaveRequestService.getLeaveRequestsByEmployee(authentication);
    }

}
