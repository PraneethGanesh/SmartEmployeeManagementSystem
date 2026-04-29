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
     * GET /totp/status
     * Returns whether TOTP is currently enabled for the logged-in employee.
     * Used by the frontend to show the correct UI state on load.
     */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        return ResponseEntity.ok(Map.of(
                "totpEnabled", employee.isTotpEnabled()
        ));
    }

    /**
     * POST /totp/setup
     * Step 1 — Generate a new TOTP secret and return the QR code URI.
     * Employee scans the QR with Google Authenticator / Authy.
     * TOTP is NOT enabled yet until /totp/verify-setup succeeds.
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication authentication) {
        Employee employee = getEmployee(authentication);

        String secret = totpService.generateSecret();
        employee.setTotpSecret(secret);
        employee.setTotpEnabled(false); // not active until confirmed
        employeeRepo.save(employee);

        String qrUri = totpService.getQrCodeUri(secret, employee.getName());

        return ResponseEntity.ok(Map.of(
                "secret", secret,          // backup code for manual entry
                "qrCodeUri", qrUri         // embed in QR image on frontend
        ));
    }

    /**
     * POST /totp/verify-setup
     * Step 2 — Employee scans QR and types their first code to confirm setup.
     * Only after this succeeds is totpEnabled set to true.
     * Body: { "code": "123456" }
     */
    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifySetup(@RequestBody Map<String, String> body,
                                         Authentication authentication) {
        Employee employee = getEmployee(authentication);

        if (employee.getTotpSecret() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No TOTP secret found. Call /totp/setup first."));
        }

        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid code. Try again."));
        }

        employee.setTotpEnabled(true);
        employeeRepo.save(employee);

        return ResponseEntity.ok(Map.of("message", "TOTP 2FA enabled successfully"));
    }

    /**
     * POST /totp/validate
     * Step 3 — On every login when totpEnabled=true, call this after password auth.
     * In the new flow this is handled by /Authenticate/totp using a pre-auth token.
     * This endpoint is kept for compatibility / manual use.
     * Body: { "code": "123456" }
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body,
                                      Authentication authentication) {
        Employee employee = getEmployee(authentication);

        if (!employee.isTotpEnabled()) {
            return ResponseEntity.ok(Map.of("message", "TOTP not enabled for this user"));
        }

        String code = body.get("code");
        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid TOTP code"));
        }

        return ResponseEntity.ok(Map.of("message", "TOTP verified", "authenticated", true));
    }

    /**
     * DELETE /totp/disable
     * Disable TOTP for the logged-in employee.
     */
    @DeleteMapping("/disable")
    public ResponseEntity<?> disable(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        employee.setTotpSecret(null);
        employee.setTotpEnabled(false);
        employeeRepo.save(employee);
        return ResponseEntity.ok(Map.of("message", "TOTP disabled"));
    }

    // ---- helper ----

    private Employee getEmployee(Authentication authentication) {
        return employeeRepo.findByName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
}