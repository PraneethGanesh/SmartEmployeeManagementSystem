// src/main/java/com/example/EmployeeManagementSystem/security/ApiKeyAuthenticationProvider.java
package com.example.EmployeeManagementSystem.security;

import com.example.EmployeeManagementSystem.Entity.ApiKey;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.ApiKeyRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationProvider(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String apiKey = null;

        // Extract API key from the authentication object
        if (authentication instanceof ApiKeyAuthenticationToken) {
            apiKey = ((ApiKeyAuthenticationToken) authentication).getApiKey();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Invalid API key");
        }

        ApiKey key = apiKeyRepository.findByApiKeyAndActiveTrue(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid or inactive API key"));

        // Check expiration
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("API key has expired");
        }

        // Update last used timestamp
        apiKeyRepository.updateLastUsed(apiKey, Instant.now());

        // Build authorities from permissions
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (key.getPermissions() != null && !key.getPermissions().isEmpty()) {
            authorities.addAll(Arrays.stream(key.getPermissions().split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }

        Object principal;
        if (key.getEmployee() != null) {
            principal = key.getEmployee();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + key.getEmployee().getRole().name()));
        } else if (key.getVendor() != null) {
            principal = key.getVendor();
            authorities.add(new SimpleGrantedAuthority("ROLE_VENDOR"));
        } else {
            throw new RuntimeException("No user associated with API key");
        }

        return new ApiKeyAuthenticationToken(principal, apiKey, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}