// src/main/java/com/example/EmployeeManagementSystem/Repository/ApiKeyRepository.java
package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByApiKeyAndActiveTrue(String apiKey);


    List<ApiKey> findByEmployee_EmployeeIdAndActiveTrue(Long employeeId);


    List<ApiKey> findByVendor_IdAndActiveTrue(Long vendorId);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :now WHERE a.apiKey = :apiKey")
    void updateLastUsed(@Param("apiKey") String apiKey, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey a SET a.active = false WHERE a.expiresAt < :now AND a.active = true")
    int expireOldKeys(@Param("now") Instant now);
}