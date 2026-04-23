package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.EmployeeDetails;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ManangerService {
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    public ManangerService(EmployeeRepo employeeRepo, PasswordEncoder passwordEncoder) {
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
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
        employee.setManager(manager.getEmail());
        employee.setTimezone(employeeDetails.getTimezone());
        return employeeRepo.save(employee);
    }
}
