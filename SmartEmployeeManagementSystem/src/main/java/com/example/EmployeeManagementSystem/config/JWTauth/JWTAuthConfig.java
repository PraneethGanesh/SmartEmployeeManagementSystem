// src/main/java/com/example/EmployeeManagementSystem/config/JWTAuth/JWTAuthConfig.java
package com.example.EmployeeManagementSystem.config.JWTauth;

import com.example.EmployeeManagementSystem.Filter.ApiKeyFilter;
import com.example.EmployeeManagementSystem.Filter.JwtAuthFilter;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Service.CustomOAuth2UserService;
import com.example.EmployeeManagementSystem.Service.RefreshTokenService;
import com.example.EmployeeManagementSystem.Util.JWTUtil;
import com.example.EmployeeManagementSystem.Util.OAuth2SuccessHandler;
import com.example.EmployeeManagementSystem.security.ApiKeyAuthenticationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class JWTAuthConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ApiKeyAuthenticationProvider apiKeyAuthProvider;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    public JWTAuthConfig(JwtAuthFilter jwtAuthFilter,
                         ApiKeyAuthenticationProvider apiKeyAuthProvider,
                         EmployeeRepo employeeRepo,
                         VendorRepo vendorRepo,
                         RefreshTokenService refreshTokenService,
                         RefreshTokenRepository refreshTokenRepository,
                         JWTUtil jwtUtil) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.apiKeyAuthProvider = apiKeyAuthProvider;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ApiKeyFilter apiKeyFilter() {
        return new ApiKeyFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            @Qualifier("combinedUserDetailService") UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(employeeRepo, vendorRepo, passwordEncoder());
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(jwtUtil, employeeRepo, vendorRepo,
                refreshTokenService, refreshTokenRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .securityContext(ctx -> ctx
                        .securityContextRepository(new DelegatingSecurityContextRepository(
                                new HttpSessionSecurityContextRepository(),
                                new RequestAttributeSecurityContextRepository()
                        ))
                )
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
//                        .maximumSessions(1)
//                        .maxSessionsPreventsLogin(false)
//                )
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints
                        .requestMatchers("/Authenticate", "/Authenticate/totp","/Authenticate/refresh").permitAll()
                        .requestMatchers("/session/login", "/session/logout").permitAll()

                        // OAuth2 endpoints
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/auth/google/init").permitAll()

                        // API docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Shared login page
                        .requestMatchers("/login.html", "/google.html").permitAll()

                        // Dashboards — served as static HTML, actual data is protected by JWT
                        .requestMatchers("/login.html",
                                "/google.html",
                                "/vendor-login.html",
                                "/vendor-dashboard.html",
                                "/tech-vendor-dashboard.html",
                                "/employee-dashboard.html",
                                "/manager-dashboard.html",
                                "/dashboard.html",
                                "/pending.html",
                                "/leave_management.html",
                                "/totp-setup.html").permitAll()

                        // Vendor pages
                        .requestMatchers("/vendor-login.html").permitAll()
                        .requestMatchers("/vendor-dashboard.html").permitAll()

                        // API key generation requires authentication
                        .requestMatchers("/api-keys/generate").authenticated()
                        .requestMatchers("/manager/createAdmin").permitAll()
                        //totp
                        .requestMatchers("/totp/**").authenticated()
                        // notification
                        .requestMatchers(HttpMethod.GET,  "/notifications").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/notifications/unread-count").authenticated()
                        .requestMatchers(HttpMethod.POST, "/notifications/mark-read").authenticated()
                        // Everything else requires a valid token
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                        .successHandler(oAuth2SuccessHandler())
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
