package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(EmployeeRepo employeeRepo,
                                   VendorRepo vendorRepo,
                                   PasswordEncoder passwordEncoder) {
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        System.out.println("=== CustomOAuth2UserService.loadUser() called ===");

        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
            System.out.println("super.loadUser() succeeded");
        } catch (Exception e) {
            System.err.println("super.loadUser() FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        System.out.println("OAuth email: " + email);

        try {
            // ── Returning user — role already set in DB, nothing to do ────────
            boolean employeeExists = employeeRepo.findByEmail(email).isPresent();
            boolean vendorExists   = vendorRepo.findByEmail(email).isPresent();
            System.out.println("employeeExists=" + employeeExists + ", vendorExists=" + vendorExists);

            if (employeeExists || vendorExists) {
                System.out.println("Returning existing user");
                return oAuth2User;
            }

            // ── Brand new user — always register as USER ──────────────────────
            // No session reading, no role param, no timezone param.
            // Role is always USER. Timezone defaults to Asia/Kolkata.
            // Admin promotes them to EMPLOYEE/MANAGER later via /admin/promote/{id}
            // Vendors register separately via POST /vendors/register (not OAuth)
            registerAsUser(email, name);

        } catch (Exception e) {
            System.err.println("=== REGISTRATION FAILED ===");
            e.printStackTrace();
            throw e;
        }

        return oAuth2User;
    }

    private void registerAsUser(String email, String name) {
        Employee e = new Employee();
        e.setEmail(email);
        e.setName(name);
        e.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        e.setRole(Role.USER);
        e.setTimezone("Asia/Kolkata");
        e.setDept("UNASSIGNED");
        employeeRepo.saveAndFlush(e); // flush immediately so OAuth2SuccessHandler sees the record
        System.out.println("New USER registered and flushed for: " + email);
    }
}