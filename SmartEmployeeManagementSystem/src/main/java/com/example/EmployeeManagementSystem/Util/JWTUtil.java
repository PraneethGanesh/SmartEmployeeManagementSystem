package com.example.EmployeeManagementSystem.Util;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JWTUtil {

    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;

    // FIX 1: Was 1000*60*60 (1 hour) — caused token expiry mid-session.
    // Extended to 24 hours to match the session timeout in application.yaml.
    private final long Expiration_time = 1000L * 60 * 60 * 24;

    @Value("${jwt.secret}")
    private String secret;
    private SecretKey key;

    public JWTUtil(EmployeeRepo employeeRepo, VendorRepo vendorRepo) {
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
    }

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username,String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role",role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + Expiration_time))
                .signWith(key)
                .compact();
    }

    public String extractUsernameFromToken(String token) {
        Claims body = extractClaims(token);
        return body.getSubject();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String username, UserDetails userDetails, String token) {
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}