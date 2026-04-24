package com.example.EmployeeManagementSystem.Util;

import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RefreshTokenRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler{
    private final JWTUtil jwtUtil;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public OAuth2SuccessHandler(JWTUtil jwtUtil,
                                EmployeeRepo employeeRepo,
                                VendorRepo vendorRepo,
                                RefreshTokenService refreshTokenService,
                                RefreshTokenRepository refreshTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        try{
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");


            // Generate refresh token — check which entity type owns this email
            RefreshToken refreshToken;
            String role;
            var employeeOpt = employeeRepo.findByEmail(email);
            var vendorOpt   = vendorRepo.findByEmail(email);

            if (employeeOpt.isPresent()) {
                role = employeeOpt.get().getRole().name();
                refreshToken = refreshTokenService.createForEmployee(employeeOpt.get());
            } else if (vendorOpt.isPresent()) {
                role = vendorOpt.get().getRole().name();
                refreshToken = refreshTokenService.createForVendor(vendorOpt.get());
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User not found post-OAuth");
                return;
            }
            String accessToken = jwtUtil.generateToken(email,role);
            refreshTokenRepository.save(refreshToken);

            // Redirect frontend to a callback page with tokens as query params
            // (or use a fragment # so tokens don't hit server logs)
            String redirectUrl = "http://localhost:8080/google.html"   // your frontend page
                    + "?accessToken="   + URLEncoder.encode(accessToken,            StandardCharsets.UTF_8)
                    + "&refreshToken="  + URLEncoder.encode(refreshToken.getToken(), StandardCharsets.UTF_8);

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
        catch (Exception e) {
            // This will now print the REAL error in your console
            System.err.println("=== OAuth2SuccessHandler FAILED ===");
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "OAuth2 login failed: " + e.getMessage());
        }
    }
}
