// src/main/java/com/example/EmployeeManagementSystem/Filter/ApiKeyFilter.java
package com.example.EmployeeManagementSystem.Filter;

import com.example.EmployeeManagementSystem.security.ApiKeyAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final AuthenticationManager authenticationManager;

    public ApiKeyFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                // Create the unauthenticated token
                ApiKeyAuthenticationToken authRequest = new ApiKeyAuthenticationToken(apiKey);
                // Run it through the provider — this hits the DB, checks expiry, loads roles
                Authentication authenticated = authenticationManager.authenticate(authRequest);
                // Only set it if validation passed
                SecurityContextHolder.getContext().setAuthentication(authenticated);
            } catch (RuntimeException e) {
                // Invalid/expired key — clear context and return 401
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/Authenticate") ||
                path.startsWith("/session/login") ||
                path.startsWith("/session/logout") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login/oauth2") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-keys/generate"); // Allow API key generation without API key
    }
}