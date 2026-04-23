package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.EmployeeDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Service
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeRepo employeeRepo;
    private final LeaveRequestRepo leaveRequestRepo;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepo employeeRepo, LeaveRequestRepo leaveRequestRepo, PasswordEncoder passwordEncoder) {
        this.employeeRepo = employeeRepo;
        this.leaveRequestRepo=leaveRequestRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    public Employee createEmployee(EmployeeDTO employeeDTO) {
        log.info("Creating new employee with email: {}", employeeDTO.getEmail());
        var employee = new Employee();
        if (employeeDTO.getName() != null)  employee.setName(employeeDTO.getName());
        if (employeeDTO.getEmail() != null) employee.setEmail(employeeDTO.getEmail());
        if (employeeDTO.getDept() != null)  employee.setDept(employeeDTO.getDept());
        if (employeeDTO.getTimezone() != null && isValidTimezone(employeeDTO.getTimezone())) {
            employee.setTimezone(employeeDTO.getTimezone());
        } else {
            employee.setTimezone("UTC");
        }
        employee.setRole(Role.EMPLOYEE);
        if(employeeDTO.getPassword()==null){
            throw new RuntimeException("Passowrd is required");
        }
        employee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        return employeeRepo.save(employee);
    }

    public Employee createManager(EmployeeDTO employeeDTO) {
        log.info("Creating new employee with email: {}", employeeDTO.getEmail());
        var employee = new Employee();
        if (employeeDTO.getName() != null)  employee.setName(employeeDTO.getName());
        if (employeeDTO.getEmail() != null) employee.setEmail(employeeDTO.getEmail());
        if (employeeDTO.getDept() != null)  employee.setDept(employeeDTO.getDept());
        if (employeeDTO.getTimezone() != null && isValidTimezone(employeeDTO.getTimezone())) {
            employee.setTimezone(employeeDTO.getTimezone());
        } else {
            employee.setTimezone("UTC");
        }
        employee.setRole(Role.MANAGER);
        if(employeeDTO.getPassword()==null){
            throw new RuntimeException("Passowrd is required");
        }
        employee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        return employeeRepo.save(employee);
    }

    public Employee updateEmployee(long id, EmployeeDTO employee) {
        var updateEmployee = employeeRepo.findById(id).orElseThrow(
                () -> new EmployeeNotFound("Employee with id:" + id + " not found")
        );

        if (employee.getName() != null)     updateEmployee.setName(employee.getName());
        if (employee.getEmail() != null)    updateEmployee.setEmail(employee.getEmail());
        if (employee.getDept() != null)     updateEmployee.setDept(employee.getDept());
        if (employee.getTimezone() != null && isValidTimezone(employee.getTimezone())) {
            updateEmployee.setTimezone(employee.getTimezone());
        }
        return employeeRepo.save(updateEmployee);
    }

    public void deleteEmployee(long id) {
        var employee = employeeRepo.findById(id).orElseThrow(
                () -> new EmployeeNotFound("Employee with id:" + id + " not found")
        );
        List<LeaveRequest> leaveRequests=leaveRequestRepo.findByEmployeeOrderByStartDateDesc(employee);
        for(LeaveRequest leaveRequest:leaveRequests){
            leaveRequestRepo.delete(leaveRequest);
        }
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
        String email= AuthUtil.extractEmail(authentication);
        Employee employee=employeeRepo.findByEmail( email).orElseThrow(
                ()-> new EmployeeNotFound("Employee :"+email+" NOT FOUND")

        );
        return employee;
    }
}