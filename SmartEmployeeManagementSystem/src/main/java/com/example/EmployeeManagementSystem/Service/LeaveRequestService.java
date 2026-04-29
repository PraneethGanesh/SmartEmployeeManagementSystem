package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.ActionDTO;
import com.example.EmployeeManagementSystem.DTO.AuthRequest;
import com.example.EmployeeManagementSystem.DTO.LeaveRequestDTO;
import com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Exception.*;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {
    private final LeaveRequestRepo leaveRequestRepo;
    private final EmployeeRepo employeeRepo;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);

    public LeaveRequestService(LeaveRequestRepo leaveRequestRepo,
                               EmployeeRepo employeeRepo,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveEntitlementRepository leaveEntitlementRepository) {
        this.leaveRequestRepo = leaveRequestRepo;
        this.employeeRepo = employeeRepo;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveEntitlementRepository = leaveEntitlementRepository;
    }

    public List<LeaveResponseDTO> getAllTheLeaveRequest(){
        List<LeaveRequest> requestList=leaveRequestRepo.findAll();
        List<LeaveResponseDTO> dtos=new ArrayList<>();
        for(LeaveRequest request:requestList){
           dtos.add(convertToDTO(request));
        }
        return dtos;
    }


    public ResponseEntity<?> createRequest(Authentication authentication, LeaveRequestDTO requestDTO){
        var leaveRequest=new LeaveRequest();
        String email= AuthUtil.extractEmail(authentication);
        //Validate employee exists and is active
        var employee=employeeRepo.findByEmail(email).orElseThrow(()->
                new EmployeeNotFound("Employee with name: "+email+" is not found")
        );
        if (employee.getStatus() != Status.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only active employees can apply for leave"));
        }

        leaveRequest.setEmployee(employee);
        //Checking if the start date is before today
        String timezone= employee.getTimezone();
        ZoneId zoneId=ZoneId.of(timezone != null && !timezone.isBlank()
                ? timezone
                : "UTC");
        LocalDate today = LocalDate.now(zoneId);
        if (requestDTO.getStartDate().isBefore(today)) {
            throw new InvalidStartDateException("Start date cannot be before current date");
        }
        //Checking if the end date is before start date
        if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())) {
            throw new InvalidEndDateException("End date must be equal to or greater than start date");
        }
        //optional
        long daysRequested = ChronoUnit.DAYS.between(requestDTO.getStartDate(), requestDTO.getEndDate()) + 1;
        if (daysRequested > 30) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Leave cannot exceed 30 consecutive days"));
        }
        //optional
        if (requestDTO.getLeaveType() == LeaveType.SICK) {
            long sickDaysUsed = leaveRequestRepo.countDaysByEmployeeAndLeaveTypeAndYear(
                    employee, LeaveType.SICK, today.getYear());

            if (sickDaysUsed + daysRequested > 12) { // Assuming 12 sick days per year
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sick leave limit exceeded for this year"));
            }
        }
        long duplicateCount= leaveRequestRepo.checkDuplicate(
                employee.getEmployeeId(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                LeaveStatus.PENDING.name()
        );
        if(duplicateCount>0){
            throw new DuplicateRequestException("Duplicate leave request");
        }

        //checking if there is any overlapping leave request of that employee
        long count= leaveRequestRepo.countOverlappingLeave(
                employee.getEmployeeId(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate()
        );
         if(count>0){
             throw new OverlappingLeaveException("Overlapping leave exists");
         }
         leaveRequest.setStartDate(requestDTO.getStartDate());
         leaveRequest.setEndDate(requestDTO.getEndDate());

         if(requestDTO.getLeaveType()==null){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body("Leave type is required..");
         }
         leaveRequest.setLeaveType(requestDTO.getLeaveType());
         if(requestDTO.getReason()!=null){
             leaveRequest.setReason(requestDTO.getReason());
         }
         LeaveRequest savedRequest=leaveRequestRepo.save(leaveRequest);
         return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedRequest));
    }

    //Only for manager
    public List<LeaveResponseDTO> getAllThePendingLeaveRequests(){
        List<LeaveRequest> requestList=leaveRequestRepo.findByStatus(LeaveStatus.PENDING);
        List<LeaveResponseDTO> responseDTOS=new ArrayList<>();
        for(LeaveRequest request:requestList){
            responseDTOS.add(convertToDTO(request));
        }
        return responseDTOS;
    }

    @Transactional
    public ResponseEntity<?> updateLeaveRequestStatus(ActionDTO actionDTO,Authentication authentication){
        String email= AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manager with name: "+email+" Not Found")
        );
        if (actionDTO.getAction() == null) {
            return ResponseEntity.badRequest().body("Action is required");
        }
        log.info("Updating leave request status for id: {}", actionDTO.getLeaveRequestId());

        var leaveRequest=leaveRequestRepo.findById(actionDTO.getLeaveRequestId()).orElseThrow(
                ()-> new LeaveRequestNotFoundException("LeaveRequest with id:"+actionDTO.getLeaveRequestId()+" not found")
        );

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body("Only PENDING requests can be approved or rejected. Current status: "
                            + leaveRequest.getStatus());
        }

         if(actionDTO.getAction().equalsIgnoreCase("approved")){
             leaveRequest.setStatus(LeaveStatus.APPROVED);
             applyApprovedLeaveToEntitlement(leaveRequest);
         }
         else if(actionDTO.getAction().equalsIgnoreCase("rejected")){
             leaveRequest.setStatus(LeaveStatus.REJECTED);
         }
         else{
             return ResponseEntity.badRequest().body("Invalid action,Use APPROVED or REJECTED");
         }
         leaveRequest.setManager(manager.getEmail());
         if(actionDTO.getRemarks()!=null){
             leaveRequest.setRemarks(actionDTO.getRemarks());
         }
         return ResponseEntity.ok(leaveRequestRepo.save(leaveRequest));
    }

    @Transactional
    public ResponseEntity<?> cancelLeaveRequest(Authentication authentication,long leaveId){
        String email= AuthUtil.extractEmail(authentication);
        var employee=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Employee with email:"+email+" not found")
        );
        var leaveRequest=leaveRequestRepo.findById(leaveId).orElseThrow(
                ()->new LeaveRequestNotFoundException("Leave request with id:"+leaveId+" is not found")
        );
        if(employee.getEmployeeId()!=leaveRequest.getEmployee().getEmployeeId()){
            return ResponseEntity.badRequest().body("You cannot cancel others Leave request..");
        }
        String timezone= employee.getTimezone();
        ZoneId zoneId=ZoneId.of(timezone != null && !timezone.isBlank()
                ? timezone
                : "UTC");
        LocalDate today = LocalDate.now(zoneId);
        if(leaveRequest.getStartDate().isEqual(today)||leaveRequest.getStartDate().isBefore(today)){
            return ResponseEntity.badRequest().body("You cannot cancel leave request after the leave has started");
        }
        LeaveStatus previousStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        if (previousStatus == LeaveStatus.APPROVED) {
            reverseApprovedLeaveFromEntitlement(leaveRequest);
        }
        leaveRequestRepo.save(leaveRequest);
        return ResponseEntity.ok("Leave Request with id:"+ leaveId+" Successfully cancelled");
    }

    private LeaveResponseDTO convertToDTO(LeaveRequest request){
        var responseDTO=new LeaveResponseDTO();
        responseDTO.setLeaveRequestId(request.getId());
        responseDTO.setEmployeeId(request.getEmployee().getEmployeeId());
        responseDTO.setLeaveType(request.getLeaveType());
        responseDTO.setStartDate(request.getStartDate());
        responseDTO.setEndDate(request.getEndDate());
        responseDTO.setNumberOfDays(requestedDays(request).longValue());
        responseDTO.setReason(request.getReason());
        responseDTO.setStatus(request.getStatus());
        return responseDTO;
    }

    public ResponseEntity<?> getLeaveRequestsByEmployee(Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        log.info("Fetching leave requests for employee: {}", email);

        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFound("Employee not found: " +email ));

        List<LeaveRequest> requests = leaveRequestRepo.findByEmployeeOrderByStartDateDesc(employee);
        List<LeaveResponseDTO> response = requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "employeeName", email,
                "totalRequests", response.size(),
                "requests", response
        ));
    }
    public List<LeaveResponseDTO> getPendingLeaves() {
        List<LeaveRequest> pendingRequests = leaveRequestRepo.findByStatus(LeaveStatus.PENDING);
        return pendingRequests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Approve a leave request by ID (simplified version for AdminController).
     * Returns LeaveResponseDTO directly.
     */
    @Transactional
    public LeaveResponseDTO approveLeave(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepo.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Leave request with id: " + id + " not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + leaveRequest.getStatus());
        }

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setManager("ADMIN"); // Or get from security context if needed
        applyApprovedLeaveToEntitlement(leaveRequest);

        LeaveRequest saved = leaveRequestRepo.save(leaveRequest);
        return convertToDTO(saved);
    }

    /**
     * Reject a leave request by ID (simplified version for AdminController).
     * Returns LeaveResponseDTO directly.
     */
    @Transactional
    public LeaveResponseDTO rejectLeave(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepo.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Leave request with id: " + id + " not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + leaveRequest.getStatus());
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setManager("ADMIN");

        LeaveRequest saved = leaveRequestRepo.save(leaveRequest);
        return convertToDTO(saved);
    }

    private void applyApprovedLeaveToEntitlement(LeaveRequest leaveRequest) {
        updateUsedLeave(leaveRequest, requestedDays(leaveRequest));
    }

    private void reverseApprovedLeaveFromEntitlement(LeaveRequest leaveRequest) {
        updateUsedLeave(leaveRequest, requestedDays(leaveRequest).negate());
    }

    private void updateUsedLeave(LeaveRequest leaveRequest, BigDecimal delta) {
        int year = leaveRequest.getStartDate().getYear();
        String leaveTypeName = leaveRequest.getLeaveType().name();
        com.example.EmployeeManagementSystem.Entity.LeaveType leaveType = leaveTypeRepository
                .findByName(leaveTypeName)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave type not configured: " + leaveTypeName));

        var entitlement = leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getEmployeeId(), leaveType.getId(), year)
                .orElseGet(() -> createEntitlement(leaveRequest.getEmployee(), leaveType, year));

        BigDecimal used = entitlement.getUsedThisYear().add(delta);
        entitlement.setUsedThisYear(used.max(BigDecimal.ZERO));
        leaveEntitlementRepository.save(entitlement);
    }

    private com.example.EmployeeManagementSystem.Entity.LeaveEntitlement createEntitlement(
            Employee employee,
            com.example.EmployeeManagementSystem.Entity.LeaveType leaveType,
            int year) {
        var entitlement = new com.example.EmployeeManagementSystem.Entity.LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(leaveType);
        entitlement.setYear(year);

        if (leaveType.isCarriesForward()) {
            BigDecimal lastYearClosing = leaveEntitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year - 1)
                    .map(com.example.EmployeeManagementSystem.Entity.LeaveEntitlement::getClosingBalance)
                    .orElse(BigDecimal.ZERO);
            entitlement.setOpeningBalance(lastYearClosing);
        }

        return leaveEntitlementRepository.save(entitlement);
    }

    private BigDecimal requestedDays(LeaveRequest leaveRequest) {
        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        return BigDecimal.valueOf(days);
    }
}
