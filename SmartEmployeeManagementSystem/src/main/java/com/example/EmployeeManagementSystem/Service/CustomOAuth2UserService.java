package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
@Component
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
        // Let Spring fetch user info from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // Read role + timezone that the frontend stored in the session
        // (we put them there during the /oauth2/authorization/google redirect)
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession session = attrs.getRequest().getSession(false);

        String roleStr  = (session != null) ? (String) session.getAttribute("oauth_role")     : null;
        String timezone = (session != null) ? (String) session.getAttribute("oauth_timezone")  : "Asia/Kolkata";

        Role userRole = Role.EMPLOYEE;
        if (roleStr != null) {
            try { userRole = Role.valueOf(roleStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        // Auto-register if first login
        boolean employeeExists = employeeRepo.findByEmail(email).isPresent();
        boolean vendorExists   = vendorRepo.findByEmail(email).isPresent();

        if (!employeeExists && !vendorExists) {
            if (userRole == Role.VENDOR) {
                Vendor v = new Vendor();
                v.setEmail(email);
                v.setName(name);
                v.setRole(Role.VENDOR);
                v.setPhone("UNAVAILABLE");
                v.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                vendorRepo.save(v);

            } else if (userRole == Role.MANAGER) {
                Employee m = new Employee();
                m.setEmail(email);
                m.setName(name);
                m.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                m.setRole(Role.MANAGER);
                m.setTimezone(timezone);
                m.setDept("UNASSIGNED");
                employeeRepo.save(m);

            } else {
                Employee e = new Employee();
                e.setEmail(email);
                e.setName(name);
                e.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                e.setRole(Role.EMPLOYEE);
                e.setTimezone(timezone);
                e.setDept("UNASSIGNED");
                employeeRepo.save(e);
            }
        }

        return oAuth2User; // Spring wraps this in its SecurityContext
    }
}
