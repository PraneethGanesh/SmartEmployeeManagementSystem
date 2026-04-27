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

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final DeliveryService.VendorService vendorService;

    public VendorController(DeliveryService.VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorDTO> registerVendor(@RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.registerVendor(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FOOD_VENDOR','ADMIN','TECH_VENDOR')")
    public ResponseEntity<List<VendorDTO>> getAllVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR_WRITE')")
    public ResponseEntity<VendorDTO> getVendor(Authentication authentication) {
        return ResponseEntity.ok(vendorService.getVendor(authentication));
    }


    @PutMapping("/update")
    @PreAuthorize("hasRole('VENDOR_WRITE')")
    public ResponseEntity<VendorDTO> updateVendor(
            @RequestBody VendorRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(vendorService.updateVendor(request, authentication));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteVendor(Authentication authentication) {
        vendorService.deleteVendor(authentication);
        return ResponseEntity.ok(
                "Vendor account for " + authentication.getName() + " has been closed.");
    }
}