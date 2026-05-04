package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import com.example.EmployeeManagementSystem.Service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    // Admin generates a key for a vendor
    @PostMapping("/vendor/{vendorId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiKey> generateVendorKey(
            @PathVariable Long vendorId,
            @RequestParam String name,
            @RequestParam(defaultValue = "VENDOR_WRITE") String permissions,
            @RequestParam(defaultValue = "365") int validDays) {
        return ResponseEntity.ok(
                apiKeyService.generateForVendor(vendorId, name, permissions, validDays));
    }

    // Revoke
    @DeleteMapping("/{keyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> revokeKey(@PathVariable Long keyId) {
        apiKeyService.revokeKey(keyId);
        return ResponseEntity.ok("Key revoked");
    }
}
