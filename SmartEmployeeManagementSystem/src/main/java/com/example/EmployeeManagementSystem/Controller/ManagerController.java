package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.EmployeeDetails;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Service.ManangerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagerController {
   private final ManangerService manangerService;

    public ManagerController(ManangerService manangerService) {
        this.manangerService = manangerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Employee createEmployee(@RequestBody EmployeeDetails employeeDetails, Authentication authentication){
       return manangerService.createEmp(employeeDetails,authentication);
    }
}
