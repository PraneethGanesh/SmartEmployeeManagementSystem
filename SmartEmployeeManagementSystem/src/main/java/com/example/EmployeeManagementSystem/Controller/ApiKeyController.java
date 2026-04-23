// src/main/java/com/example/EmployeeManagementSystem/Controller/ApiKeyController.java
package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.ApiKeyRepository;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    
    private final ApiKeyRepository apiKeyRepository;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;
    
    public ApiKeyController(ApiKeyRepository apiKeyRepository, 
                            EmployeeRepo employeeRepo,
                            VendorRepo vendorRepo) {
        this.apiKeyRepository = apiKeyRepository;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
    }
    
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'VENDOR')")
    public ResponseEntity<Map<String, Object>> generateApiKey(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String keyName = request.get("keyName");
        String permissions = request.get("permissions");
        Integer expiryDays = request.get("expiryDays") != null ? 
                Integer.parseInt(request.get("expiryDays")) : 30;
        
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyName(keyName);
        apiKey.setPermissions(permissions);
        
        // Find the user
        Employee employee = employeeRepo.findByEmail(authentication.getName()).orElse(null);
        if (employee != null) {
            apiKey.setEmployee(employee);
        } else {
            Vendor vendor = vendorRepo.findByEmail(authentication.getName()).orElse(null);
            apiKey.setVendor(vendor);
        }
        
        apiKey.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
        
        ApiKey saved = apiKeyRepository.save(apiKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("apiKey", saved.getApiKey());
        response.put("message", "API key generated successfully. Save this key - it won't be shown again!");
        response.put("expiresAt", saved.getExpiresAt());
        
        return ResponseEntity.ok(response);
    }

    // In ApiKeyController.java - update these methods:

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'VENDOR')")
    public ResponseEntity<List<ApiKey>> getMyApiKeys(Authentication authentication) {
        Employee employee = employeeRepo.findByEmail(authentication.getName()).orElse(null);
        if (employee != null) {
            // FIX: Use corrected method name
            return ResponseEntity.ok(apiKeyRepository.findByEmployee_EmployeeIdAndActiveTrue(employee.getEmployeeId()));
        }

        Vendor vendor = vendorRepo.findByEmail(authentication.getName()).orElse(null);
        if (vendor != null) {
            // FIX: Use corrected method name
            return ResponseEntity.ok(apiKeyRepository.findByVendor_IdAndActiveTrue(vendor.getId()));
        }

        return ResponseEntity.badRequest().build();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'VENDOR')")
    public ResponseEntity<Map<String, String>> revokeApiKey(@PathVariable Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey != null) {
            apiKey.setActive(false);
            apiKeyRepository.save(apiKey);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "API key revoked successfully");
        return ResponseEntity.ok(response);
    }
}