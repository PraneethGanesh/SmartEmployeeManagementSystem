package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import com.example.EmployeeManagementSystem.Repository.ApiKeyRepository;
import com.example.EmployeeManagementSystem.Service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;  // ← instance, lowercase

    public ApiKeyController(ApiKeyService apiKeyService,
                            ApiKeyRepository apiKeyRepository) {
        this.apiKeyService = apiKeyService;
        this.apiKeyRepository = apiKeyRepository;
    }

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

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApiKey>> getVendorKeys(@PathVariable Long vendorId) {
        return ResponseEntity.ok(apiKeyRepository.findByVendor_IdAndActiveTrue(vendorId));
    }

    @DeleteMapping("/{keyId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> revokeKey(@PathVariable Long keyId) {
        apiKeyRepository.findById(keyId).ifPresent(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
        });
        return ResponseEntity.ok("Key revoked.");
    }
}