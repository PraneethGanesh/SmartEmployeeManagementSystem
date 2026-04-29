package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AccessTokenRequest;
import com.example.EmployeeManagementSystem.DTO.AuthRequest;
import com.example.EmployeeManagementSystem.DTO.AuthResponse;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Service.RefreshTokenService;
import com.example.EmployeeManagementSystem.Service.TotpService;
import com.example.EmployeeManagementSystem.Util.JWTUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/Authenticate")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TotpService totpService;
    private final EmployeeRepo employeeRepo;

    // Temporary store for pre-auth tokens (username -> pre-auth token)
    // In production, use Redis or DB-backed store with TTL
    private static final ConcurrentHashMap<String, PreAuthEntry> PRE_AUTH_STORE = new ConcurrentHashMap<>();

    private record PreAuthEntry(String username, String role, long expiresAt) {}

    public AuthController(AuthenticationManager authenticationManager,
                          JWTUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          RefreshTokenRepository refreshTokenRepository,
                          TotpService totpService,
                          EmployeeRepo employeeRepo) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.totpService = totpService;
        this.employeeRepo = employeeRepo;
    }

    /**
     * Step 1 of login. If TOTP is enabled, returns a pre-auth token instead of a JWT.
     * The client must then call /Authenticate/totp with that pre-auth token + 6-digit code.
     */
    @PostMapping
    public ResponseEntity<?> authenticateToken(@RequestBody AuthRequest authRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            UserDetails user = (UserDetails) auth.getPrincipal();

            Employee employee = employeeRepo.findByEmail(authRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            String role = user.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .map(a -> a.substring("ROLE_".length()))
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            // Check if TOTP is enabled for this user
            if (employee.isTotpEnabled()) {
                String preAuthToken = UUID.randomUUID().toString();
                PRE_AUTH_STORE.put(preAuthToken, new PreAuthEntry(
                        employee.getEmail(),   // store email, not display name
                        role,
                        System.currentTimeMillis() + 5 * 60 * 1000L
                ));
                return ResponseEntity.ok(Map.of(
                        "requiresTotp", true,
                        "preAuthToken", preAuthToken
                ));
            }

            // TOTP not enabled — issue JWT directly (existing behavior)
            return ResponseEntity.ok(issueTokens(user, role));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Step 2 of login when TOTP is enabled.
     * Submit the pre-auth token + 6-digit TOTP code to get the real JWT.
     */
    @PostMapping("/totp")
    public ResponseEntity<?> completeTotpLogin(@RequestBody Map<String, String> body) {
        String preAuthToken = body.get("preAuthToken");
        String code = body.get("code");

        if (preAuthToken == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "preAuthToken and code are required"));
        }

        PreAuthEntry entry = PRE_AUTH_STORE.get(preAuthToken);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            PRE_AUTH_STORE.remove(preAuthToken);
            return ResponseEntity.status(401).body(Map.of("error", "Pre-auth token expired or invalid. Please login again."));
        }

        Employee employee = employeeRepo.findByEmail(entry.username())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid TOTP code"));
        }

        // TOTP verified — consume the pre-auth token and issue JWT
        PRE_AUTH_STORE.remove(preAuthToken);
        return ResponseEntity.ok(issueTokens(employee, entry.role()));
    }

    /**
     * Refresh token endpoint — unchanged from original.
     */
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
            role = storedToken.getEmployee().getRole().name();
        } else if (storedToken.getVendor() != null) {
            username = storedToken.getVendor().getUsername();
            role = storedToken.getVendor().getRole().name();
        } else {
            throw new RuntimeException("Refresh token has no associated user");
        }

        String newAccessToken = jwtUtil.generateToken(username, role);
        return new AuthResponse(newAccessToken, storedToken.getToken());
    }

    // ---- helper ----

    private AuthResponse issueTokens(UserDetails user, String role) {
        String accessToken = jwtUtil.generateToken(user.getUsername(), role);
        RefreshToken refreshToken;
        if (user instanceof Employee employee) {
            refreshToken = refreshTokenService.createForEmployee(employee);
        } else if (user instanceof Vendor vendor) {
            refreshToken = refreshTokenService.createForVendor(vendor);
        } else {
            throw new RuntimeException("Unknown user type");
        }
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }
}