package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.AuthResponse;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Service.CombinedUserDetailService;
import com.example.EmployeeManagementSystem.Service.RefreshTokenService;
import com.example.EmployeeManagementSystem.Util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth/google")
public class OauthController {

    @GetMapping("/init")
    public void initOAuth(@RequestParam(defaultValue = "EMPLOYEE") String role,
                          @RequestParam(defaultValue = "Asia/Kolkata") String timezone,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(true);
        session.setAttribute("oauth_role",     role.toUpperCase());
        session.setAttribute("oauth_timezone", timezone);

        // Now hand off to Spring's built-in OAuth2 flow
        response.sendRedirect("/oauth2/authorization/google");
    }
}
