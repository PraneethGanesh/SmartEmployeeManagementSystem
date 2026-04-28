package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.EmployeeDTO;
import com.example.EmployeeManagementSystem.DTO.VendorDTO;
import com.example.EmployeeManagementSystem.DTO.VendorRequest;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final EmployeeService employeeService;
    private final DeliveryService.VendorService vendorService;
    private final LeaveRequestService leaveRequestService;
    private final RestaurantService restaurantService;
    private final SubscriptionService subscriptionService;
    private final LeaveAccrualService accrualService;
    private final SickLeaveResetService resetService;

    public AdminController(
            EmployeeService employeeService,
            DeliveryService.VendorService vendorService,
            LeaveRequestService leaveRequestService,
            RestaurantService restaurantService,
            SubscriptionService subscriptionService,
            LeaveAccrualService accrualService,
            SickLeaveResetService resetService) {
        this.employeeService = employeeService;
        this.vendorService = vendorService;
        this.leaveRequestService = leaveRequestService;
        this.restaurantService = restaurantService;
        this.subscriptionService = subscriptionService;
        this.accrualService = accrualService;
        this.resetService=resetService;
    }

    // ─────────────────────────────────────────────────────────────
    // EMPLOYEE BRANCH
    // ─────────────────────────────────────────────────────────────

    /** List every employee in the system. */
    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    /** Create a regular employee. */
    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(dto));
    }

    /** Promote / create a manager. */
    @PostMapping("/employees/manager")
    public ResponseEntity<Employee> createManager(@RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createManager(dto));
    }

    /** Update any employee record. */
    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable long id,
            @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }

    /** Hard-delete an employee. */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee with id: " + id + " has been deleted.");
    }

    /** Soft-deactivate an employee (sets status to INACTIVE). */
    @PutMapping("/employees/{id}/inactive")
    public ResponseEntity<String> deactivateEmployee(@PathVariable long id) {
        return employeeService.inactivateUser(id);
    }

    // ─────────────────────────────────────────────────────────────
    // VENDOR BRANCH  (only admin can register vendors)
    // ─────────────────────────────────────────────────────────────

    /** List every vendor. */
    @GetMapping("/vendors")
    public ResponseEntity<List<VendorDTO>> getAllVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @PostMapping("/vendors")
    public ResponseEntity<VendorDTO> registerVendor(@RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.registerVendor(request));
    }

    /** Remove a vendor by ID. */
    @DeleteMapping("/vendors/{vendorId}")
    public ResponseEntity<String> deleteVendor(@PathVariable Long vendorId) {
        vendorService.deleteVendorById(vendorId);
        return ResponseEntity.ok("Vendor with id: " + vendorId + " has been removed.");
    }

    // ─────────────────────────────────────────────────────────────
    // LEAVE MANAGEMENT BRANCH
    // ─────────────────────────────────────────────────────────────

    /** Fetch all pending leave requests across the organisation. */
    @GetMapping("/leaves/pending")
    public ResponseEntity<?> getPendingLeaves() {
        return ResponseEntity.ok(leaveRequestService.getPendingLeaves());
    }

    /** Approve a leave request. */
    @PutMapping("/leaves/{id}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.approveLeave(id));
    }

    /** Reject a leave request. */
    @PutMapping("/leaves/{id}/reject")
    public ResponseEntity<?> rejectLeave(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.rejectLeave(id));
    }

    // ─────────────────────────────────────────────────────────────
    // RESTAURANT MANAGEMENT BRANCH
    // ─────────────────────────────────────────────────────────────

    /** View all restaurants. */
    @GetMapping("/restaurants")
    public ResponseEntity<?> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants());
    }

    // ─────────────────────────────────────────────────────────────
    // SUBSCRIPTION MANAGEMENT BRANCH
    // ─────────────────────────────────────────────────────────────

    /** View all active subscriptions. */
    @GetMapping("/subscriptions")
    public ResponseEntity<?> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @PostMapping("/leave/accrue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerAccrual(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate accrualDate = (date != null) ? date : LocalDate.now();
        accrualService.runMonthlyAccrual(accrualDate);
        return ResponseEntity.ok("Accrual complete for " + accrualDate);
    }
    @PostMapping("/reset-sick")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerSickReset(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate resetDate = (date != null) ? date : LocalDate.now();
        resetService.runMonthlyReset(resetDate);
        return ResponseEntity.ok("Sick leave reset complete for " + resetDate);
    }

}