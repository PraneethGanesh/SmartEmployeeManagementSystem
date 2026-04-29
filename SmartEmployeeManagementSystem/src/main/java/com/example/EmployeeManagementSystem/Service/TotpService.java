package com.example.EmployeeManagementSystem.Service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    /** Generate a new Base32 secret for a user */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate a QR code URI — user scans this with Google Authenticator / Authy.
     * Format: otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}
     */
    public String getQrCodeUri(String secret, String username) {
        return String.format(
                "otpauth://totp/EmployeeMS:%s?secret=%s&issuer=EmployeeManagementSystem",
                username, secret
        );
    }

    /**
     * Verify the 6-digit code the user typed.
     * DefaultCodeVerifier already allows ±1 time step (±30 seconds) for clock skew.
     */
    public boolean verifyCode(String secret, String code) {
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            return false;
        }
    }
}