package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Service.TotpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/totp")
public class TotpController {

    private final TotpService totpService;
    private final EmployeeRepo employeeRepo;

    public TotpController(TotpService totpService, EmployeeRepo employeeRepo) {
        this.totpService = totpService;
        this.employeeRepo = employeeRepo;
    }

    /**
     * Step 1 — Employee calls this to get their QR code URI.
     * They scan it with Google Authenticator / Authy.
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication authentication) {
        Employee employee = employeeRepo.findByName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String secret = totpService.generateSecret();
        employee.setTotpSecret(secret);
        employee.setTotpEnabled(false); // not enabled until verified
        employeeRepo.save(employee);

        String qrUri = totpService.getQrCodeUri(secret, employee.getName());

        return ResponseEntity.ok(Map.of(
                "secret", secret,          // show this as backup code
                "qrCodeUri", qrUri         // embed this in a QR image on frontend
        ));
    }

    /**
     * Step 2 — Employee scans QR, types their first code to confirm setup.
     * Only after this is totpEnabled = true.
     */
    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifySetup(@RequestBody Map<String, String> body,
                                         Authentication authentication) {
        Employee employee = employeeRepo.findByName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String code = body.get("code");
        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid code. Try again."));
        }

        employee.setTotpEnabled(true);
        employeeRepo.save(employee);

        return ResponseEntity.ok(Map.of("message", "TOTP 2FA enabled successfully"));
    }

    /**
     * Step 3 — On every login, if totpEnabled=true, call this after Basic Auth succeeds.
     * Returns 200 if code is valid (you can then issue a session or JWT).
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body,
                                      Authentication authentication) {
        Employee employee = employeeRepo.findByName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!employee.isTotpEnabled()) {
            return ResponseEntity.ok(Map.of("message", "TOTP not enabled for this user"));
        }

        String code = body.get("code");
        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid TOTP code"));
        }

        return ResponseEntity.ok(Map.of("message", "TOTP verified", "authenticated", true));
    }

    /** Disable TOTP for the logged-in employee */
    @DeleteMapping("/disable")
    public ResponseEntity<?> disable(Authentication authentication) {
        Employee employee = employeeRepo.findByName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setTotpSecret(null);
        employee.setTotpEnabled(false);
        employeeRepo.save(employee);

        return ResponseEntity.ok(Map.of("message", "TOTP disabled"));
    }
}