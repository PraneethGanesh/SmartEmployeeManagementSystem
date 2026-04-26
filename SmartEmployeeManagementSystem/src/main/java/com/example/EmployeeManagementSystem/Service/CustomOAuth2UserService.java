package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        System.out.println("OAuth email: " + email);

        boolean employeeExists = employeeRepo.findByEmail(email).isPresent();
        boolean vendorExists   = vendorRepo.findByEmail(email).isPresent();
        System.out.println("employeeExists=" + employeeExists + ", vendorExists=" + vendorExists);

        List<GrantedAuthority> authorities = new   ArrayList<>(oAuth2User.getAuthorities());

        if (employeeExists) {
            Employee employee = employeeRepo.findByEmail(email).get();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
            System.out.println("Returning existing employee with role: " + employee.getRole());
            return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
        }

        if (vendorExists) {
            // add vendor role if needed
            authorities.add(new SimpleGrantedAuthority("ROLE_VENDOR"));
            System.out.println("Returning existing vendor");
            return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
        }

        // New user
        registerAsUser(email, name);
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
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