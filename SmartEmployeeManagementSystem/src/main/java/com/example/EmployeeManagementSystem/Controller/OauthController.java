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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth/google")
public class OauthController {
    private final EmployeeRepo employeeRepo;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    public OauthController(EmployeeRepo employeeRepo, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.employeeRepo = employeeRepo;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String client_id;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String client_secret;

    @Autowired
    private VendorRepo vendorRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    CombinedUserDetailService combinedUserDetailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtil jwtUtil;


    @GetMapping("/callback")
    public ResponseEntity<?> handelGoogleCallback(@RequestParam String code,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false, defaultValue = "Asia/Kolkata") String timezone){
        try {
            System.out.println("CODE RECEIVED: " + code);
            System.out.println("STATE RECEIVED: " + role);
            // Parse role from state parameter (sent from frontend)
            Role userRole = Role.EMPLOYEE; // default role
            if(role != null && !role.isEmpty()) {
                try {
                    userRole = Role.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid role: " + role);
                }
            }
            System.out.println("USER ROLE: " + userRole);

            String tokenEndpoint="https://oauth2.googleapis.com/token";
            MultiValueMap<String,String> params=new LinkedMultiValueMap<>();
            params.add("code",code);
            params.add("client_id",client_id);
            params.add("client_secret",client_secret);
            params.add("redirect_uri","http://localhost:8080/google.html");
            params.add("grant_type","authorization_code");

            HttpHeaders headers=new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String,String>> request=new HttpEntity<>(params,headers);
            ResponseEntity<Map> tokenResponse=restTemplate.postForEntity(tokenEndpoint,request,Map.class);
            String idToken=(String) tokenResponse.getBody().get("id_token");
            String UserInfoUrl="https://oauth2.googleapis.com/tokeninfo?id_token="+idToken;
            ResponseEntity<Map> userInfoResponse=restTemplate.getForEntity(UserInfoUrl, Map.class);

            if(userInfoResponse.getStatusCode()== HttpStatus.OK){
                Map<String,Object> userInfo=userInfoResponse.getBody();
                String email=(String) userInfo.get("email");
                UserDetails userDetails=null;

                try{
                    userDetails=combinedUserDetailService.loadUserByUsername(email);
                } catch (UsernameNotFoundException e) {
                    if (userRole == Role.VENDOR) {

                        Vendor vendor = new Vendor();
                        vendor.setEmail(email);
                        vendor.setName((String) userInfo.get("name"));
                        vendor.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                        vendorRepo.save(vendor);

                    } else if (userRole == Role.MANAGER) {

                        Employee manager = new Employee();
                        manager.setEmail(email);
                        manager.setName((String) userInfo.get("name"));
                        manager.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        manager.setRole(Role.MANAGER);
                        manager.setTimezone(timezone);
                        manager.setDept("UNASSIGNED");
                        employeeRepo.save(manager);

                    } else { // EMPLOYEE

                        Employee employee = new Employee();
                        employee.setEmail(email);
                        employee.setName((String) userInfo.get("name"));
                        employee.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        employee.setRole(Role.EMPLOYEE);
                        employee.setDept("UNASSIGNED");
                        employee.setTimezone(timezone);
                        employeeRepo.save(employee);
                    }

                    userDetails = combinedUserDetailService.loadUserByUsername(email);
                }
                String accessToken= jwtUtil.generateToken(email);
                RefreshToken refreshToken;

                if (userDetails instanceof Employee employee) {
                    refreshToken = refreshTokenService.createForEmployee(employee);
                } else if (userDetails instanceof Vendor vendor) {
                    refreshToken = refreshTokenService.createForVendor(vendor);
                } else {
                    throw new RuntimeException("Unknown user type");
                }

                refreshTokenRepository.save(refreshToken);

                AuthResponse response = new AuthResponse(accessToken, refreshToken.getToken());

                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
