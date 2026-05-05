package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.*;
import com.example.EmployeeManagementSystem.Exception.*;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.slf4j.Logger;
import com.example.EmployeeManagementSystem.Enum.LeaveType;   // the Enum — used everywhere in logic
// Entity LeaveType is always referenced fully qualified in the code below — no import needed
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
    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);

    public LeaveRequestService(LeaveRequestRepo leaveRequestRepo,
                               EmployeeRepo employeeRepo,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveEntitlementRepository leaveEntitlementRepository,
                                NotificationService notificationService) {
        this.leaveRequestRepo = leaveRequestRepo;
        this.employeeRepo = employeeRepo;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveEntitlementRepository = leaveEntitlementRepository;
        this.notificationService=notificationService;
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

        if (requestDTO.getLeaveType() == LeaveType.MATERNITY) {
            if(employee.getGender()==Gender.M){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Maternity leave is not applicable");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Please use the maternity leave application process.");
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
        // ---------------- SICK ----------------
        if (requestDTO.getLeaveType() == LeaveType.SICK) {

            if (daysRequested > 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sick leave is limited to 1 day per month."));
            }

            long sickDaysUsedThisMonth = leaveRequestRepo.countApprovedSickDaysInMonth(
                    employee, today.getYear(), today.getMonthValue());

            var sickLeaveType = leaveTypeRepository.findByName("SICK")
                    .orElseThrow(() -> new RuntimeException("SICK leave type not configured"));

            var entitlement = leaveEntitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(),
                            sickLeaveType.getId(),
                            today.getYear())
                    .orElseGet(() -> createEntitlement(employee, sickLeaveType, today.getYear()));

            BigDecimal availableSick = entitlement.getAvailableBalance();

            if (sickDaysUsedThisMonth >= 1 || availableSick.compareTo(BigDecimal.ONE) < 0) {
                requestDTO.setLeaveType(LeaveType.UNPAID);
            }
        }


// ---------------- CASUAL ----------------
        if (requestDTO.getLeaveType() == LeaveType.CASUAL) {

            com.example.EmployeeManagementSystem.Entity.LeaveType leaveType=leaveTypeRepository.findByName("CASUAL").orElseThrow(
                    ()->new RuntimeException("Leave type Not found")
            );

            int leaveYear=requestDTO.getStartDate().getYear();
            LeaveEntitlement leaveEntitlement=leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                    employee.getEmployeeId(),
                    leaveType.getId(),
                    leaveYear
            ).orElseGet(()->createEntitlement(employee,leaveType,leaveYear));

            BigDecimal availableCasual = leaveEntitlement.getAvailableBalance(); // opening + accrued - used
            BigDecimal requestedCasual = BigDecimal.valueOf(daysRequested);
            if (availableCasual.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No casual leave balance available."));
            }

            if (requestedCasual.compareTo(availableCasual) > 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Requested casual leave exceeds available balance. Available: " + availableCasual
                        ));
            }

        }

        // ---------------- UNPAID cap ----------------
        // An employee may take at most 1 unpaid day per month — the same quota
        // as the leave type it replaces. This applies whether the employee
        // submitted UNPAID directly or was auto-downgraded from SICK/CASUAL.
        if (requestDTO.getLeaveType() == LeaveType.UNPAID) {
            long unpaidDaysThisMonth = leaveRequestRepo.countUnpaidDaysInMonth(
                    employee, today.getYear(), today.getMonthValue());
            if (unpaidDaysThisMonth >= 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Unpaid leave limit (1 day/month) already reached. "
                                        + "No further leave can be taken this month."));
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
        LeaveType originalLeaveType = requestDTO.getLeaveType();
         leaveRequest.setLeaveType(requestDTO.getLeaveType());
         if(requestDTO.getReason()!=null){
             leaveRequest.setReason(requestDTO.getReason());
         }
         LeaveRequest savedRequest=leaveRequestRepo.save(leaveRequest);

        Employee manager = employee.getManager(); // assuming getManager() exists
        if (manager != null) {
            notificationService.notify(
                    manager,
                    employee.getName() + " submitted a " + savedRequest.getLeaveType() ,
                    "LEAVE_REQUEST"
            );
        }
        // After the existing manager notification block
        employeeRepo.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.notify(
                        admin,
                        employee.getName() + " submitted a " + requestDTO.getLeaveType() + " leave request (" +
                                requestDTO.getStartDate() + " → " + requestDTO.getEndDate() + ")",
                        "LEAVE_REQUEST"
                )
        );
        LeaveResponseDTO responseDTO = convertToDTO(savedRequest);
        if (savedRequest.getLeaveType() == LeaveType.UNPAID && savedRequest.getLeaveType() != originalLeaveType) {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("leave", responseDTO);
            body.put("warning", "Your " + originalLeaveType.name().toLowerCase() +
                    " leave quota for this month is exhausted. Request recorded as Unpaid leave.");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
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
             notificationService.notify(
                     leaveRequest.getEmployee(),
                     "Your leave request from " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + " has been approved.",
                     "LEAVE_APPROVED"
             );
             employeeRepo.findByRole(Role.ADMIN).forEach(admin ->
                     notificationService.notify(
                             admin,
                             leaveRequest.getEmployee().getName() + "'s leave request has been approved by " + manager.getName(),
                             "LEAVE_APPROVED"
                     )
             );
         }
         else if(actionDTO.getAction().equalsIgnoreCase("rejected")){
             leaveRequest.setStatus(LeaveStatus.REJECTED);
             notificationService.notify(
                     leaveRequest.getEmployee(),
                     "Your leave request from " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + " was rejected." +
                             (actionDTO.getRemarks() != null ? " Reason: " + actionDTO.getRemarks() : ""),
                     "LEAVE_REJECTED"
             );
             employeeRepo.findByRole(Role.ADMIN).forEach(admin ->
                     notificationService.notify(
                             admin,
                             leaveRequest.getEmployee().getName() + "'s leave request has been rejected by " + manager.getName(),
                             "LEAVE_REJECTED"
                     )
             );
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
        responseDTO.setEmployeeName(request.getEmployee().getName());
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
        notificationService.notify(
                leaveRequest.getEmployee(),
                "Your leave request from " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + " has been approved by admin.",
                "LEAVE_APPROVED"
        );

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
        notificationService.notify(
                leaveRequest.getEmployee(),
                "Your leave request from " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + " was rejected by admin.",
                "LEAVE_REJECTED"
        );

        LeaveRequest saved = leaveRequestRepo.save(leaveRequest);
        return convertToDTO(saved);
    }

    private void applyApprovedLeaveToEntitlement(LeaveRequest leaveRequest) {
        if (leaveRequest.getLeaveType() == LeaveType.MATERNITY) {
            grantAndDeductMaternity(leaveRequest);  // special maternity flow
        } else {
            ensureSufficientBalanceForApproval(leaveRequest);
            updateUsedLeave(leaveRequest, requestedDays(leaveRequest)); // existing flow
        }
    }

    private void grantAndDeductMaternity(LeaveRequest leaveRequest) {
        int year = leaveRequest.getStartDate().getYear();
        Employee employee = leaveRequest.getEmployee();

        com.example.EmployeeManagementSystem.Entity.LeaveType maternityType =
                leaveTypeRepository.findByName("MATERNITY")
                        .orElseThrow(() -> new IllegalStateException(
                                "Maternity leave type not configured"));

        var entitlement = leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                        employee.getEmployeeId(), maternityType.getId(), year)
                .orElseGet(() -> createEntitlement(employee, maternityType, year));

        // Grant 180 days one time on approval
        entitlement.setAccruedThisYear(BigDecimal.valueOf(180));

        // Deduct the days she is taking (always 180)
        entitlement.setUsedThisYear(requestedDays(leaveRequest));

        leaveEntitlementRepository.save(entitlement);
    }

    private void reverseApprovedLeaveFromEntitlement(LeaveRequest leaveRequest) {
        if (leaveRequest.getLeaveType() == LeaveType.MATERNITY) {
            reverseMaternityEntitlement(leaveRequest); // wipe the grant entirely
        } else {
            updateUsedLeave(leaveRequest, requestedDays(leaveRequest).negate());
        }
    }

    private void reverseMaternityEntitlement(LeaveRequest leaveRequest) {
        int year = leaveRequest.getStartDate().getYear();
        Employee employee = leaveRequest.getEmployee();

        com.example.EmployeeManagementSystem.Entity.LeaveType maternityType =
                leaveTypeRepository.findByName("MATERNITY")
                        .orElseThrow(() -> new IllegalStateException(
                                "Maternity leave type not configured"));

        leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                        employee.getEmployeeId(), maternityType.getId(), year)
                .ifPresent(entitlement -> {
                    // Wipe both the grant and the usage
                    entitlement.setAccruedThisYear(BigDecimal.ZERO);
                    entitlement.setUsedThisYear(BigDecimal.ZERO);
                    leaveEntitlementRepository.save(entitlement);
                });
    }

    private void updateUsedLeave(LeaveRequest leaveRequest, BigDecimal delta) {
        int year = leaveRequest.getStartDate().getYear();
        String leaveTypeName = leaveRequest.getLeaveType().name();
        var leaveType = leaveTypeRepository
                .findByName(leaveTypeName)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave type not configured: " + leaveTypeName));

        var entitlement = leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getEmployeeId(), leaveType.getId(), year)
                .orElseGet(() -> createEntitlement(leaveRequest.getEmployee(), leaveType, year));

        BigDecimal newUsed = entitlement.getUsedThisYear().add(delta);

// Prevent negative
        newUsed = newUsed.max(BigDecimal.ZERO);

// Cap used days to the actual entitled total for the year
        BigDecimal totalEntitled = entitlement.getOpeningBalance().add(entitlement.getAccruedThisYear());
        if (newUsed.compareTo(totalEntitled) > 0) {
            newUsed = totalEntitled;
        }

        entitlement.setUsedThisYear(newUsed);
        leaveEntitlementRepository.save(entitlement);
    }

    private void ensureSufficientBalanceForApproval(LeaveRequest leaveRequest) {
        int year = leaveRequest.getStartDate().getYear();
        String leaveTypeName = leaveRequest.getLeaveType().name();
        var leaveType = leaveTypeRepository
                .findByName(leaveTypeName)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave type not configured: " + leaveTypeName));

        var entitlement = leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getEmployeeId(), leaveType.getId(), year)
                .orElseGet(() -> createEntitlement(leaveRequest.getEmployee(), leaveType, year));

        BigDecimal availableBalance = entitlement.getAvailableBalance();
        BigDecimal requestedDays = requestedDays(leaveRequest);

        if (requestedDays.compareTo(availableBalance) > 0) {
            throw new IllegalStateException(
                    "Insufficient " + leaveTypeName.toLowerCase() + " leave balance for approval. " +
                            "Available: " + availableBalance + ", requested: " + requestedDays);
        }
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
