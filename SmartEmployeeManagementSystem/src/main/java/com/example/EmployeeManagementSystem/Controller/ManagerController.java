package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager")
public class ManagerController {
   private final ManagerService manangerService;

    public ManagerController(ManagerService manangerService) {
        this.manangerService = manangerService;
    }

    @PostMapping("/createAdmin")
    public Employee createAdmin(@RequestBody EmployeeDetails employeeDetails){
        return manangerService.createAdmin(employeeDetails);
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public Employee createEmployee(@RequestBody EmployeeDetails employeeDetails, Authentication authentication){
       return manangerService.createEmp(employeeDetails,authentication);
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('MANAGER')")
    public List<Employee> getAllEmployeeByManager(Authentication authentication){
        return manangerService.getAllEmployeeByManager(authentication);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('MANAGER')")
    public List<ManagerLeaveResponseDTO> getAllLeaveRequestByManager(Authentication authentication){
        return manangerService.getAllLeaveRequestByManager(authentication);
    }

    @GetMapping("/Users")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<EmployeeDTO> getUsers(){
        return manangerService.getUsers();
    }

    @PutMapping("/promote")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> promoteUser(Authentication authentication, @RequestBody PromoteRequest promoteRequest){
         return manangerService.promoteUser(authentication,promoteRequest);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<LeaveResponseDTO>> getAllTheLeaveRequests(Authentication authentication) {
        return ResponseEntity.ok(manangerService.getAllThePendingLeaveRequests(authentication));
    }



}
