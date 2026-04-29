package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.LeaveDashboardResponse;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Service.LeaveDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api")
public class LeaveDashboardController {

    private final LeaveDashboardService dashboardService;
    private final EmployeeRepo employeeRepository;

    public LeaveDashboardController(LeaveDashboardService dashboardService,
                                    EmployeeRepo employeeRepository) {
        this.dashboardService   = dashboardService;
        this.employeeRepository = employeeRepository;
    }

    // ── GET own dashboard (any authenticated employee) ───────
    // GET /api/employees/me/leave-dashboard
    @GetMapping("/employees/me/leave-dashboard")
    public ResponseEntity<LeaveDashboardResponse> getMyDashboard(
            Authentication authentication) {

        Long callerId = resolveEmployeeId(authentication);
        return ResponseEntity.ok(
                dashboardService.getDashboard(callerId, callerId, resolveRole(authentication)));
    }

    // ── GET any employee's dashboard (manager/admin only) ────
    // GET /api/employees/5/leave-dashboard
    @GetMapping("/employees/{id}/leave-dashboard")
    public ResponseEntity<LeaveDashboardResponse> getEmployeeDashboard(
            @PathVariable Long id,
            Authentication authentication) {

        Long   callerId   = resolveEmployeeId(authentication);
        String callerRole = resolveRole(authentication);
        return ResponseEntity.ok(
                dashboardService.getDashboard(id, callerId, callerRole));
    }

    // ── GET team dashboard (manager/admin only) ──────────────
    // GET /api/employees/me/team-dashboard
    @GetMapping("/employees/me/team-dashboard")
    public ResponseEntity<List<LeaveDashboardResponse>> getTeamDashboard(
            Authentication authentication) {

        Long   callerId   = resolveEmployeeId(authentication);
        String callerRole = resolveRole(authentication);
        return ResponseEntity.ok(
                dashboardService.getTeamDashboard(callerId, callerRole));
    }

    // ── PATCH mark warning read ───────────────────────────────
    // PATCH /api/warnings/42/read
    @PatchMapping("/warnings/{warningId}/read")
    public ResponseEntity<Void> markWarningRead(
            @PathVariable Long warningId,
            Authentication authentication) {

        Long   callerId   = resolveEmployeeId(authentication);
        String callerRole = resolveRole(authentication);
        dashboardService.markWarningRead(warningId, callerId, callerRole);
        return ResponseEntity.noContent().build();
    }

    // Supports both JWT/session UserDetails and Google OAuth2 principals.
    private Long resolveEmployeeId(Authentication authentication) {
        String email = resolveEmail(authentication);
        return employeeRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"))
                .getEmployeeId();
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .map(authority -> authority.replace("ROLE_", ""))
                .orElse("EMPLOYEE");
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        throw new ResponseStatusException(UNAUTHORIZED, "Authenticated email not found");
    }
}
