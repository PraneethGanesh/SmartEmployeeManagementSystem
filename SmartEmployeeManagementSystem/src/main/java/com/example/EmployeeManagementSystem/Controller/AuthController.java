package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AccessTokenRequest;
import com.example.EmployeeManagementSystem.DTO.AuthRequest;
import com.example.EmployeeManagementSystem.DTO.AuthResponse;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Service.RefreshTokenService;
import com.example.EmployeeManagementSystem.Util.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/Authenticate")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping
    public AuthResponse authenticateToken(@RequestBody AuthRequest authRequest) {
        try {
            System.out.println("1. Trying to authenticate user: " + authRequest.getUsername());
            System.out.println("2. Password length: " + authRequest.getPassword().length());

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            System.out.println("3. Authentication successful! User: " + auth.getName());
            System.out.println("4. Authorities: " + auth.getAuthorities());

            UserDetails user = (UserDetails) auth.getPrincipal();

            String role = user.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .findFirst()
                    .map(authority -> authority.substring("ROLE_".length()))
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            String accessToken = jwtUtil.generateToken(user.getUsername(),role);

            RefreshToken refreshToken;

            if (user instanceof Employee employee) {
                refreshToken = refreshTokenService.createForEmployee(employee);
            } else if (user instanceof Vendor vendor) {
                refreshToken = refreshTokenService.createForVendor(vendor);
            } else {
                throw new RuntimeException("Unknown user type");
            }

            refreshTokenRepository.save(refreshToken);
            System.out.println("5. Token generated: " + accessToken);

            return new AuthResponse(accessToken, refreshToken.getToken());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();  // This will show full stack trace
            throw new RuntimeException(e);
        }


    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody AccessTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (storedToken.getExpiryTime().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired");
        }

        String username;
        String role;

        if (storedToken.getEmployee() != null) {
            username = storedToken.getEmployee().getUsername();
            role=storedToken.getEmployee().getRole().name();
        } else if (storedToken.getVendor() != null) {
            username = storedToken.getVendor().getUsername();
            role=storedToken.getVendor().getRole().name();
        } else {
            throw new RuntimeException("Refresh token has no owner");
        }

        String newAccessToken = jwtUtil.generateToken(username,role);

        return new AuthResponse(newAccessToken, storedToken.getToken());
    }
}
