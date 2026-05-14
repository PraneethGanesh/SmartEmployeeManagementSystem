package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.EmployeeCreatedEvent;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepo employeeRepo;
    private final LeaveRequestRepo leaveRequestRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository; // FIX: added
    private final LeaveAccrualService leaveAccrualService;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeService(EmployeeRepo employeeRepo,
                           LeaveRequestRepo leaveRequestRepo,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenRepository refreshTokenRepository,
                           LeaveAccrualService leaveAccrualService, ApplicationEventPublisher eventPublisher) {
        this.employeeRepo = employeeRepo;
        this.leaveRequestRepo = leaveRequestRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository; // FIX: injected
        this.leaveAccrualService = leaveAccrualService;
        this.eventPublisher = eventPublisher;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    @Transactional
    public Employee createEmployee(AdminEmployeeDTO employeeDTO) {
        log.info("Creating new employee with email: {}", employeeDTO.getEmail());
        var employee = new Employee();
        if (employeeDTO.getName() != null) employee.setName(employeeDTO.getName());
        if (employeeDTO.getEmail() != null) employee.setEmail(employeeDTO.getEmail());
        if (employeeDTO.getTimezone() != null && isValidTimezone(employeeDTO.getTimezone())) {
            employee.setTimezone(employeeDTO.getTimezone());
        } else {
            employee.setTimezone("UTC");
        }
        employee.setRole(Role.EMPLOYEE);
        if (employeeDTO.getPassword() == null) {
            throw new RuntimeException("Password is required");
        }
        String rawPassword=employeeDTO.getPassword();
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employee.setGender(employeeDTO.getGender());
        Employee manager=employeeRepo.findById(employeeDTO.getManagerId()).orElseThrow(
                ()->new EmployeeNotFound("Employee with not found"+ employeeDTO.getManagerId())
        );
        employee.setDept(manager.getDept());
        employee.setManager(manager);
        employee.setResetToken(UUID.randomUUID().toString());
        employee.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        Employee savedEmployee = employeeRepo.save(employee);
        leaveAccrualService.grantInitialMonthlyAccrual(savedEmployee, savedEmployee.getJoined_at());

        eventPublisher.publishEvent(new EmployeeCreatedEvent(savedEmployee,rawPassword));
        return savedEmployee;
    }

    @Transactional
    public Employee createManager(EmployeeDTO employeeDTO) {
        log.info("Creating new manager with email: {}", employeeDTO.getEmail());
        var employee = new Employee();
        if (employeeDTO.getName() != null) employee.setName(employeeDTO.getName());
        if (employeeDTO.getEmail() != null) employee.setEmail(employeeDTO.getEmail());
        if (employeeDTO.getDept() != null) employee.setDept(employeeDTO.getDept());
        if (employeeDTO.getTimezone() != null && isValidTimezone(employeeDTO.getTimezone())) {
            employee.setTimezone(employeeDTO.getTimezone());
        } else {
            employee.setTimezone("UTC");
        }
        employee.setRole(Role.MANAGER);
        if (employeeDTO.getPassword() == null) {
            throw new RuntimeException("Password is required");
        }
        employee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        employee.setGender(employeeDTO.getGender());
        Employee savedEmployee = employeeRepo.save(employee);
        leaveAccrualService.grantInitialMonthlyAccrual(savedEmployee, savedEmployee.getJoined_at());
        return savedEmployee;
    }

    public Employee updateEmployee(long id, EmployeeDTO employee) {
        var updateEmployee = employeeRepo.findById(id).orElseThrow(
                () -> new EmployeeNotFound("Employee with id:" + id + " not found")
        );
        if (employee.getName() != null)
            updateEmployee.setName(employee.getName());
        if (employee.getEmail() != null)
            updateEmployee.setEmail(employee.getEmail());
        if (employee.getDept() != null)
            updateEmployee.setDept(employee.getDept());
        if (employee.getTimezone() != null && isValidTimezone(employee.getTimezone())) {
            updateEmployee.setTimezone(employee.getTimezone());
        }
        return employeeRepo.save(updateEmployee);
    }

    /**
     * FIX: Delete order must be:
     *  1. refresh_token  (FK → employee)
     *  2. leave_request  (FK → employee)
     *  3. employee
     *
     * Previously only leave_request was deleted first, causing a
     * FK constraint failure on refresh_token when deleting the employee.
     */
    @Transactional
    public void deleteEmployee(long id) {
        var employee = employeeRepo.findById(id).orElseThrow(
                () -> new EmployeeNotFound("Employee with id:" + id + " not found")
        );

        // Step 1: delete refresh tokens for this employee
        refreshTokenRepository.deleteByEmployee(employee);

        // Step 2: delete leave requests for this employee
        List<LeaveRequest> leaveRequests =
                leaveRequestRepo.findByEmployeeOrderByStartDateDesc(employee);
        leaveRequestRepo.deleteAll(leaveRequests);

        // Step 3: delete the employee
        employeeRepo.delete(employee);
    }

    public ResponseEntity<String> inactivateUser(long employeeId) {
        var employee = employeeRepo.findById(employeeId).orElseThrow(
                () -> new EmployeeNotFound("Employee with id:" + employeeId + " not found")
        );
        if (employee.getStatus() == Status.INACTIVE) {
            return ResponseEntity.badRequest()
                    .body("Status of the employee with id: " + employeeId + " is already set to inactive");
        }
        employee.setStatus(Status.INACTIVE);
        employeeRepo.save(employee);
        return ResponseEntity.ok("Status of the employee with id: " + employeeId + " is set to inactive");
    }

    /** Validate that the given string is a recognized IANA timezone id. */
    private boolean isValidTimezone(String tz) {
        try {
            ZoneId.of(tz);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Employee getAccount(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email).orElseThrow(
                () -> new EmployeeNotFound("Employee :" + email + " NOT FOUND")
        );
        return employee;
    }

    public EmployeeAttendanceResponseDTO getEmployeeAttendanceOverview() {
        List<Employee> employees = employeeRepo.findAll();

        List<EmployeeStatusDTO> workingEmployees = employees.stream()
                .filter(e -> e.getStatus() == Status.ACTIVE)
                .map(this::toStatusDTO)
                .collect(Collectors.toList());

        List<EmployeeStatusDTO> onLeaveEmployees = employees.stream()
                .filter(e -> e.getStatus() == Status.ON_LEAVE)
                .map(this::toStatusDTO)
                .collect(Collectors.toList());

        EmployeeAttendanceResponseDTO response = new EmployeeAttendanceResponseDTO();
        response.setWorkingCount(workingEmployees.size());
        response.setOnLeaveCount(onLeaveEmployees.size());
        response.setWorkingEmployees(workingEmployees);
        response.setOnLeaveEmployees(onLeaveEmployees);
        return response;
    }

    private EmployeeStatusDTO toStatusDTO(Employee employee) {
        EmployeeStatusDTO dto = new EmployeeStatusDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDept(employee.getDept());
        dto.setRole(employee.getRole());
        return dto;
    }

    public ResponseEntity<List<ManagerDTO>> getAllManagers() {
        List<ManagerDTO> managers = employeeRepo.findByRole(Role.MANAGER).stream()
                .map(this::toManagerDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(managers);
    }

    private ManagerDTO toManagerDTO(Employee employee) {
        ManagerDTO dto = new ManagerDTO();
        dto.setManagerId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDept(employee.getDept());
        return dto;
    }

    public String promoteEmployee(long employeeId) {
        Employee employee=employeeRepo.findById(employeeId).orElseThrow(
                ()->new EmployeeNotFound("Employee Not Found:"+employeeId)
        );
        employee.setRole(Role.MANAGER);
        employee.setManager(null);
        Employee saved=employeeRepo.save(employee);
        return "Employee:"+saved.getName()+" is promoted to Manager";
    }

    public List<EmployeeDTO> getAllEmployeesByRole() {
        List<Employee> employees=employeeRepo.findByRole(Role.EMPLOYEE);
        return employees.stream().map(employee -> convertToDTO(employee)).toList();
    }
    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
       dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDept(employee.getDept());
        dto.setPassword(employee.getPassword());
        dto.setGender(employee.getGender());
        dto.setTimezone(employee.getTimezone());

        return dto;
    }
}
