package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.TotpUser;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Service.TotpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/totp")
public class TotpController {

    private final TotpService totpService;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;

    public TotpController(TotpService totpService, EmployeeRepo employeeRepo, VendorRepo vendorRepo) {
        this.totpService = totpService;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication authentication) {
        TotpUser user = getSubject(authentication);
        return ResponseEntity.ok(Map.of("totpEnabled", user.isTotpEnabled()));
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication authentication) {
        TotpUser user = getSubject(authentication);

        if (user.isTotpEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "TOTP already enabled. Disable first."));
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        save(user);

        String qrUri = totpService.getQrCodeUri(secret, user.getName());
        return ResponseEntity.ok(Map.of("secret", secret, "qrCodeUri", qrUri));
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifySetup(@RequestBody Map<String, String> body,
                                         Authentication authentication) {
        TotpUser user = getSubject(authentication);

        if (user.getTotpSecret() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No TOTP secret found. Call /totp/setup first."));
        }

        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid code. Try again."));
        }

        user.setTotpEnabled(true);
        save(user);
        return ResponseEntity.ok(Map.of("message", "TOTP 2FA enabled successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body,
                                      Authentication authentication) {
        TotpUser user = getSubject(authentication);

        if (!user.isTotpEnabled()) {
            return ResponseEntity.ok(Map.of("message", "TOTP not enabled for this user"));
        }

        if (user.getTotpSecret() == null) {
            return ResponseEntity.status(500).body(Map.of("error", "TOTP secret missing"));
        }

        String code = body.get("code");
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid TOTP code"));
        }

        return ResponseEntity.ok(Map.of("message", "TOTP verified", "authenticated", true));
    }

    @DeleteMapping("/disable")
    public ResponseEntity<?> disable(Authentication authentication) {
        TotpUser user = getSubject(authentication);
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        save(user);
        return ResponseEntity.ok(Map.of("message", "TOTP disabled"));
    }

    // ---- helpers ----

    private TotpUser getSubject(Authentication authentication) {
        String email = authentication.getName();
        return employeeRepo.findByEmail(email)
                .map(e -> (TotpUser) e)
                .orElseGet(() -> vendorRepo.findByEmail(email)
                        .map(v -> (TotpUser) v)
                        .orElseThrow(() -> new RuntimeException("User not found")));
    }

    private void save(TotpUser user) {
        if (user instanceof Employee e) employeeRepo.save(e);
        else if (user instanceof Vendor v) vendorRepo.save(v);
    }
}