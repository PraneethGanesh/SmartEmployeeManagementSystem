package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.ActionDTO;
import com.example.EmployeeManagementSystem.DTO.LeaveRequestDTO;
import com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO;
import com.example.EmployeeManagementSystem.DTO.MaternityLeaveRequestDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Service.LeaveRequestService;
import com.example.EmployeeManagementSystem.Service.MaternityLeaveService;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("leave_requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final MaternityLeaveService maternityLeaveService;
    private final EmployeeRepo employeeRepo;
    private final LeaveRequestRepo leaveRequestRepo;

    public LeaveRequestController(LeaveRequestService leaveRequestService, MaternityLeaveService maternityLeaveService, EmployeeRepo employeeRepo, LeaveRequestRepo leaveRequestRepo) {
        this.leaveRequestService = leaveRequestService;
        this.maternityLeaveService = maternityLeaveService;
        this.employeeRepo = employeeRepo;
        this.leaveRequestRepo = leaveRequestRepo;
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

    @PostMapping("/maternity/apply")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<?> applyMaternityLeave(
            Authentication authentication,
            @RequestBody MaternityLeaveRequestDTO dto) {
        return maternityLeaveService.applyMaternityLeave(authentication, dto);
    }


    // In LeaveRequestController (or wherever your leave endpoints live)
    @GetMapping("/sick/used-this-month")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> sickLeaveUsedThisMonth(
            Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Employee emp = employeeRepo.findByEmail(email)
                .orElseThrow();

        LocalDate now = LocalDate.now();
        boolean used = leaveRequestRepo.existsApprovedSickLeaveForMonth(
                emp.getEmployeeId(), now.getYear(), now.getMonthValue());

        return ResponseEntity.ok(Map.of(
                "usedThisMonth", used,
                "month", now.getMonth().toString()
        ));
    }

}
