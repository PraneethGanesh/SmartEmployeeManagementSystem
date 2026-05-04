package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.ApiKeyRepository;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;
    @Autowired private EmployeeRepo employeeRepo;
    @Autowired private VendorRepo vendorRepo;

    @Transactional
    public ApiKey generateForVendor(Long vendorId, String name,
                                    String permissions, int validDays) {
        Vendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        ApiKey key = new ApiKey();  // constructor auto-generates "ems_<uuid>"
        key.setVendor(vendor);
        key.setKeyName(name);
        key.setPermissions(permissions);  // e.g. "VENDOR_WRITE" or "TECH_VENDOR"
        key.setExpiresAt(Instant.now().plus(validDays, ChronoUnit.DAYS));
        key.setActive(true);
        return apiKeyRepository.save(key);
    }

    // Same for employee-issued keys if needed
    public List<ApiKey> getActiveKeysForVendor(Long vendorId) {
        return apiKeyRepository.findByVendor_IdAndActiveTrue(vendorId);
    }

    @Transactional
    public void revokeKey(Long keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("Key not found"));
        key.setActive(false);
        apiKeyRepository.save(key);
    }
}
