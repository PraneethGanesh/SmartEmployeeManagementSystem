package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO;
import com.example.EmployeeManagementSystem.DTO.MaternityLeaveRequestDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.*;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Exception.InvalidStartDateException;
import com.example.EmployeeManagementSystem.Exception.OverlappingLeaveException;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class MaternityLeaveService {

    private final EmployeeRepo employeeRepo;
    private final LeaveRequestRepo leaveRequestRepo;
    private final NotificationService notificationService;

    public MaternityLeaveService(EmployeeRepo employeeRepo, LeaveRequestRepo leaveRequestRepo, NotificationService notificationService) {
        this.employeeRepo = employeeRepo;
        this.leaveRequestRepo = leaveRequestRepo;
        this.notificationService = notificationService;
    }


    public ResponseEntity<?> applyMaternityLeave(Authentication authentication,
                                                 MaternityLeaveRequestDTO maternityLeaveRequestDTO){
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFound("Employee not found: " + email));

        if (employee.getStatus() != Status.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only active employees can apply"));
        }

        if (employee.getGender() != Gender.F) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only female employees can apply for maternity leave"));
        }

        boolean alreadyApplied = leaveRequestRepo
                .existsMaternityLeaveForYear(
                        employee,
                        LeaveType.MATERNITY,
                        maternityLeaveRequestDTO.getStartDate().getYear(),
                        List.of(LeaveStatus.CANCELLED,LeaveStatus.REJECTED));
        if (alreadyApplied) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maternity leave already applied for this year"));
        }


        LocalDate today = LocalDate.now();
        if (maternityLeaveRequestDTO.getStartDate().isBefore(today)) {
            throw new InvalidStartDateException("Start date cannot be before today");
        }

        // 5. Overlapping check
        LocalDate endDate = maternityLeaveRequestDTO.getStartDate().plusDays(179); // always 180 days
        long overlapping = leaveRequestRepo.countOverlappingLeave(
                employee.getEmployeeId(),
                maternityLeaveRequestDTO.getStartDate(),
                endDate);
        if (overlapping > 0) {
            throw new OverlappingLeaveException("Overlapping leave exists");
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(LeaveType.MATERNITY);
        request.setStartDate(maternityLeaveRequestDTO.getStartDate());
        request.setEndDate(endDate);          // forced 180 days
        request.setStatus(LeaveStatus.PENDING);
        request.setReason(maternityLeaveRequestDTO.getReason());

        LeaveRequest saved = leaveRequestRepo.save(request);
        Employee manager = employee.getManager();
        if (manager != null) {
            notificationService.notify(
                    manager,
                    employee.getName() + " has applied for maternity leave.",
                    "MATERNITY_LEAVE_REQUEST"
            );
        }
        employeeRepo.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.notify(
                        admin,
                        employee.getName() + " applied for maternity leave (" +
                                maternityLeaveRequestDTO.getStartDate() + " → " + endDate + ")",
                        "MATERNITY_LEAVE_REQUEST"
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    private LeaveResponseDTO convertToDTO(LeaveRequest request){
        var responseDTO=new LeaveResponseDTO();
        responseDTO.setLeaveRequestId(request.getId());
        responseDTO.setEmployeeName(request.getEmployee().getName());
        responseDTO.setLeaveType(request.getLeaveType());
        responseDTO.setStartDate(request.getStartDate());
        responseDTO.setEndDate(request.getEndDate());
        responseDTO.setNumberOfDays(requestedDays(request).longValue());
        responseDTO.setReason(request.getReason());
        responseDTO.setStatus(request.getStatus());
        return responseDTO;
    }

    private BigDecimal requestedDays(LeaveRequest leaveRequest) {
        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        return BigDecimal.valueOf(days);
    }
}
