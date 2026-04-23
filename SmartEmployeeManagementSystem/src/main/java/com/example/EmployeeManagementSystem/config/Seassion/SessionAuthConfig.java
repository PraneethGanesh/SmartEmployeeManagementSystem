//package com.example.EmployeeManagementSystem.config.Seassion;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
//
//@Configuration
//public class SessionAuthConfig {
//
//    @Bean
//    @Order(1)
//    public SecurityFilterChain sessionFilterChain(HttpSecurity http) throws Exception {
//        http
//                .securityMatcher("/session/**", "/web/**")
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/session/login", "/session/logout").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session
//                        .maximumSessions(1)           // one session per user
//                        .maxSessionsPreventsLogin(false) // new login kicks old session
//                )
//                .securityContext(ctx -> ctx
//                        .securityContextRepository(new HttpSessionSecurityContextRepository())
//                );
//        return http.build();
//    }
//}