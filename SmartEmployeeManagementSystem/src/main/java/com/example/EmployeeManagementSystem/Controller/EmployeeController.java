package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.EmployeeDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<?> getAllEmployees(){
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MANAGER','EMPLOYEE')")
    public ResponseEntity<Employee> myAccount(Authentication authentication){
        return ResponseEntity.ok(employeeService.getAccount(authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable long id,@RequestBody EmployeeDTO employeeDTO){
        return ResponseEntity.ok(employeeService.updateEmployee(id,employeeDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable long id){
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee with id: "+id+" is deleted");
    }

    //should be done by manager
    @PutMapping("/inactive")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> inactivateEmployee(@RequestParam long employeeId) {
        return employeeService.inactivateUser(employeeId);
    }
}
