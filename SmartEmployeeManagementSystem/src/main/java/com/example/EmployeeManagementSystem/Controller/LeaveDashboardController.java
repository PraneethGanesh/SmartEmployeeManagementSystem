package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.LeaveDashboardResponse;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Service.LeaveDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @AuthenticationPrincipal UserDetails caller) {

        Long callerId = resolveEmployeeId(caller);
        return ResponseEntity.ok(
                dashboardService.getDashboard(callerId, callerId, resolveRole(caller)));
    }

    // ── GET any employee's dashboard (manager/admin only) ────
    // GET /api/employees/5/leave-dashboard
    @GetMapping("/employees/{id}/leave-dashboard")
    public ResponseEntity<LeaveDashboardResponse> getEmployeeDashboard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails caller) {

        Long   callerId   = resolveEmployeeId(caller);
        String callerRole = resolveRole(caller);
        return ResponseEntity.ok(
                dashboardService.getDashboard(id, callerId, callerRole));
    }

    // ── GET team dashboard (manager/admin only) ──────────────
    // GET /api/employees/me/team-dashboard
    @GetMapping("/employees/me/team-dashboard")
    public ResponseEntity<List<LeaveDashboardResponse>> getTeamDashboard(
            @AuthenticationPrincipal UserDetails caller) {

        Long   callerId   = resolveEmployeeId(caller);
        String callerRole = resolveRole(caller);
        return ResponseEntity.ok(
                dashboardService.getTeamDashboard(callerId, callerRole));
    }

    // ── PATCH mark warning read ───────────────────────────────
    // PATCH /api/warnings/42/read
    @PatchMapping("/warnings/{warningId}/read")
    public ResponseEntity<Void> markWarningRead(
            @PathVariable Long warningId,
            @AuthenticationPrincipal UserDetails caller) {

        Long   callerId   = resolveEmployeeId(caller);
        String callerRole = resolveRole(caller);
        dashboardService.markWarningRead(warningId, callerId, callerRole);
        return ResponseEntity.noContent().build();
    }

    // ── Resolve caller from Spring Security context ───────────
    // Adapt these two methods to however your UserDetails stores
    // the employee ID and role (e.g. a custom UserDetails impl)
    private Long resolveEmployeeId(UserDetails caller) {
        return employeeRepository
                .findByEmail(caller.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"))
                .getEmployeeId();
    }

    private String resolveRole(UserDetails caller) {
        return caller.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("EMPLOYEE");
    }
}