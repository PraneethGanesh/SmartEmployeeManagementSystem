package com.example.EmployeeManagementSystem.Util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {
    public static String extractEmail(Authentication authentication) {
        if (authentication == null) return null;

        // OAuth2 login (session-based) — principal is OAuth2User
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email"); // always the actual email
        }

        // JWT or session username/password login — getName() is already the email
        return authentication.getName();
    }
}
