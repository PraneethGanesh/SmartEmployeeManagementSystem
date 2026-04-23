package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, EmployeeRepo employeeRepo, VendorRepo vendorRepo) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.employeeRepo = employeeRepo;
        this.vendorRepo= vendorRepo;
    }

    public RefreshToken createForEmployee(Employee employee) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryTime(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshToken.setEmployee(employee);
        return refreshToken;
    }

    public RefreshToken createForVendor(Vendor vendor) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryTime(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshToken.setVendor(vendor);
        return refreshToken;
    }
}
