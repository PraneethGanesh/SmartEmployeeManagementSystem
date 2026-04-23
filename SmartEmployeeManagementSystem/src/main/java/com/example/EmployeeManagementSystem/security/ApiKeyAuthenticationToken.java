// src/main/java/com/example/EmployeeManagementSystem/security/ApiKeyAuthenticationToken.java
package com.example.EmployeeManagementSystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class ApiKeyAuthenticationToken implements Authentication {

    private static final long serialVersionUID = 1L;

    private final Object principal;
    private final Object credentials;
    private final String apiKey;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;

    // Constructor for unauthenticated API key (before validation)
    public ApiKeyAuthenticationToken(String apiKey) {
        this.apiKey = apiKey;
        this.principal = null;
        this.credentials = null;
        this.authorities = null;
        this.authenticated = false;
    }

    // Constructor for authenticated API key (after validation)
    public ApiKeyAuthenticationToken(Object principal, Object credentials,
                                     Collection<? extends GrantedAuthority> authorities) {
        this.apiKey = null;
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = authorities;
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        if (principal instanceof com.example.EmployeeManagementSystem.Entity.Employee) {
            return ((com.example.EmployeeManagementSystem.Entity.Employee) principal).getUsername();
        }
        if (principal instanceof com.example.EmployeeManagementSystem.Entity.Vendor) {
            return ((com.example.EmployeeManagementSystem.Entity.Vendor) principal).getUsername();
        }
        return principal != null ? principal.toString() : apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }
}