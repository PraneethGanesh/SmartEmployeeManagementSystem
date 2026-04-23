package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.VendorDTO;
import com.example.EmployeeManagementSystem.DTO.VendorRequest;
import com.example.EmployeeManagementSystem.Service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vendor management endpoints.
 *
 * POST   /vendors              — Register a new vendor (public / admin)
 * GET    /vendors              — List all vendors (admin)
 * GET    /vendors/{id}         — Get a specific vendor
 * PUT    /vendors/{id}         — Update vendor details (vendor-only)
 * DELETE /vendors/{id}         — Remove vendor (admin)
 *
 * NOTE: Until Spring Security is wired in, role checks are done manually in the
 *       service layer. Employees and managers have NO access to /vendors/** write operations
 *       — the service will throw UnauthorizedAccessException if called with wrong role context.
 */
@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final DeliveryService.VendorService vendorService;

    public VendorController(DeliveryService.VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/register")
    public ResponseEntity<VendorDTO> registerVendor(@RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.registerVendor(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<VendorDTO>> getAllVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorDTO> getVendor(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendor(authentication));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorDTO> updateVendor(@RequestBody VendorRequest request,Authentication authentication) {
        return ResponseEntity.ok(vendorService.updateVendor(request,authentication));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> deleteVendor(Authentication authentication) {
        vendorService.deleteVendor(authentication);
        return ResponseEntity.ok("Vendor with email " + authentication.getName() + " has been removed.");
    }
}