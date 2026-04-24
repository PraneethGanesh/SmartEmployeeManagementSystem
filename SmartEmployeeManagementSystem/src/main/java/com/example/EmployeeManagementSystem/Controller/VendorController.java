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
 * Vendor self-service API — used exclusively by authenticated VENDOR accounts.
 *
 * Vendors are created by the Admin via POST /admin/vendors (see AdminController).
 * Vendors log in through the dedicated /vendor-login.html page.
 *
 * Endpoints:
 *   GET  /vendors/me        — get own vendor profile
 *   PUT  /vendors/update    — update own profile
 *   DELETE /vendors         — delete own account
 *
 * Registration and listing endpoints have been moved to AdminController.
 * All endpoints here require ROLE_VENDOR only.
 */
@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final DeliveryService.VendorService vendorService;

    public VendorController(DeliveryService.VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('Admin')")
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

    /**
     * Vendor updates their own profile details (name, contact, etc.).
     * Credentials (email/password) changes should go through a separate
     * secure flow; this endpoint handles non-sensitive fields.
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorDTO> updateVendor(
            @RequestBody VendorRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(vendorService.updateVendor(request, authentication));
    }

    /**
     * Vendor deletes (closes) their own account.
     * Admin can remove a vendor via DELETE /admin/vendors/{id}.
     */
    @DeleteMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> deleteVendor(Authentication authentication) {
        vendorService.deleteVendor(authentication);
        return ResponseEntity.ok(
                "Vendor account for " + authentication.getName() + " has been closed.");
    }
}