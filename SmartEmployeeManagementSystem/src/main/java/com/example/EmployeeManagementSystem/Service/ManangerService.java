package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.EmployeeDTO;
import com.example.EmployeeManagementSystem.DTO.EmployeeDetails;
import com.example.EmployeeManagementSystem.DTO.PromoteRequest;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManangerService {
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final LeaveRequestRepo leaveRequestRepo;

    public ManangerService(EmployeeRepo employeeRepo, PasswordEncoder passwordEncoder, LeaveRequestRepo leaveRequestRepo) {
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
        this.leaveRequestRepo = leaveRequestRepo;
    }

    public Employee createEmp(EmployeeDetails employeeDetails, Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Employee manager= employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manager :"+email+"Not found")
        );
        Employee employee=new Employee();
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        employee.setDept(manager.getDept());
        employee.setRole(Role.EMPLOYEE);
        employee.setManager(manager);
        employee.setTimezone(employeeDetails.getTimezone());
        return employeeRepo.save(employee);
    }


    public List<Employee> getAllEmployeeByManager(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manger:"+email+" not found")
        );
        return employeeRepo.findByManager(manager);
    }

    public List<LeaveRequest> getAllLeaveRequestByManager(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
       Employee manager=employeeRepo.findByEmail(email).orElseThrow(
               ()->new EmployeeNotFound("Manager with "+email+" not found")
       );
       return leaveRequestRepo.findByEmployee_Manager(manager);
    }

    public List<EmployeeDTO> getUsers(){
        List<Employee> employeeList=employeeRepo.findByRole(Role.USER);
        return employeeList.stream().map(this::toDTO).toList();
    }

    public ResponseEntity<?> promoteUser(Authentication authentication, PromoteRequest promoteRequest){
        Employee user=employeeRepo.findByEmail(promoteRequest.getEmail()).orElseThrow(
                ()->new EmployeeNotFound("Employee:"+promoteRequest.getEmail()+" not found")
        );
        if(user.getRole()!=Role.USER){
            return ResponseEntity.ok("Employee already promoted");
        }
        String managerEmail=AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(managerEmail).orElseThrow(
                ()->new EmployeeNotFound("Manager:"+managerEmail+" not found")
        );

        user.setRole(Role.EMPLOYEE);
        user.setManager(manager);
        user.setDept(manager.getDept());
        user.setPassword(passwordEncoder.encode(promoteRequest.getPassword()));
        Employee promotedEmployee=employeeRepo.save(user);
        return ResponseEntity.ok(toDTO(promotedEmployee));
    }
    
    public EmployeeDTO toDTO(Employee employee){
        EmployeeDTO employeeDTO=new EmployeeDTO();
        employeeDTO.setName(employee.getName());
        employeeDTO.setEmail(employee.getEmail());
        employeeDTO.setDept(employee.getDept());
        employeeDTO.setPassword(employee.getPassword());
        employeeDTO.setTimezone(employee.getTimezone());
        return employeeDTO;
    }

    public Employee createAdmin(EmployeeDetails employeeDetails) {
        Employee employee=new Employee();
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        employee.setDept("Main");
        employee.setRole(Role.ADMIN);
        employee.setTimezone(employeeDetails.getTimezone());
        return employeeRepo.save(employee);
    }
}
