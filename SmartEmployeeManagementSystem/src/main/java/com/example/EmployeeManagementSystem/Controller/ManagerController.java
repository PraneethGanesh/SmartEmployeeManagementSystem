package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.EmployeeDetails;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Service.ManangerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/manager")
public class ManagerController {
   private final ManangerService manangerService;

    public ManagerController(ManangerService manangerService) {
        this.manangerService = manangerService;
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('MANAGER')")
    public Employee createEmployee(@RequestBody EmployeeDetails employeeDetails, Authentication authentication){
       return manangerService.createEmp(employeeDetails,authentication);
    }

    @GetMapping
    public List<Employee> getAllEmployeeByManager(Authentication authentication){
        return manangerService.getAllEmployeeByManager(authentication);
    }
}
