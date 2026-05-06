package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Service.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/session")
public class SessionAuthController {

    private final AuthenticationManager authenticationManager;
    private final TotpService totpService;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private static final ConcurrentHashMap<String, PreAuthSession> PRE_AUTH_STORE = new ConcurrentHashMap<>();

    private record PreAuthSession(Authentication authentication, long expiresAt) {}

    public SessionAuthController(AuthenticationManager authenticationManager,
                                 TotpService totpService,
                                 EmployeeRepo employeeRepo,
                                 VendorRepo vendorRepo) {
        this.authenticationManager = authenticationManager;
        this.totpService = totpService;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String secret = getTotpSecret(auth);
            if (secret != null) {
                String preAuthToken = UUID.randomUUID().toString();
                PRE_AUTH_STORE.put(preAuthToken, new PreAuthSession(
                        auth,
                        System.currentTimeMillis() + 5 * 60 * 1000L
                ));
                return ResponseEntity.ok(Map.of(
                        "requiresTotp", true,
                        "preAuthToken", preAuthToken
                ));
            }

            saveAuthentication(auth, request, response);
            HttpSession session = request.getSession(false);
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", auth.getName(),
                    "sessionId", session != null ? session.getId() : "",
                    "roles", auth.getAuthorities().toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials: " + e.getMessage()));
        }
    }

    @PostMapping("/totp")
    public ResponseEntity<?> completeTotpLogin(@RequestBody Map<String, String> body,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        String preAuthToken = body.get("preAuthToken");
        String code = body.get("code");

        if (preAuthToken == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "preAuthToken and code are required"));
        }

        PreAuthSession entry = PRE_AUTH_STORE.get(preAuthToken);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            PRE_AUTH_STORE.remove(preAuthToken);
            return ResponseEntity.status(401).body(Map.of("error", "token expired"));
        }

        String secret = getTotpSecret(entry.authentication());
        if (secret == null || !totpService.verifyCode(secret, code)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid TOTP code"));
        }

        PRE_AUTH_STORE.remove(preAuthToken);
        saveAuthentication(entry.authentication(), request, response);
        HttpSession session = request.getSession(false);
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "username", entry.authentication().getName(),
                "sessionId", session != null ? session.getId() : "",
                "roles", entry.authentication().getAuthorities().toString()
        ));
    }

    private void saveAuthentication(Authentication auth,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
            // Create and set security context
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // CRITICAL: Save the context to the HTTP session
            HttpSession session = request.getSession(true);
            securityContextRepository.saveContext(context, request, response);

            // Alternative: Manual session attribute (redundant but safe)
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private String getTotpSecret(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Employee employee && employee.isTotpEnabled()) {
            return employeeRepo.findByEmail(employee.getEmail())
                    .map(Employee::getTotpSecret)
                    .orElse(employee.getTotpSecret());
        }
        if (principal instanceof Vendor vendor && vendor.isTotpEnabled()) {
            return vendorRepo.findByEmail(vendor.getEmail())
                    .map(Vendor::getTotpSecret)
                    .orElse(vendor.getTotpSecret());
        }
        return null;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        // Invalidate Spring Security context
        SecurityContextHolder.clearContext();

        // Invalidate HTTP session (kills OAuth2 session too)
        session.invalidate();

        // Clear the session cookie
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", authentication.getAuthorities().toString(),
                "authenticated", true
        ));
    }
}
