package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.LeaveDashboardResponse;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveWarning;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveWarningRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveDashboardService {

    private final EmployeeRepo employeeRepository;
    private final LeaveEntitlementRepository leaveEntitlementRepository; // was not final
    private final LeaveWarningRepository warningRepository;

    public LeaveDashboardService(EmployeeRepo employeeRepository,
                                 LeaveEntitlementRepository leaveEntitlementRepository, // parameter name fixed
                                 LeaveWarningRepository warningRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveEntitlementRepository = leaveEntitlementRepository; // was "entitlementRepository" — mismatch!
        this.warningRepository = warningRepository;
    }

    @Transactional(readOnly = true)
    public LeaveDashboardResponse getDashboard(Long requestedEmployeeId,
                                               Long callerEmployeeId,
                                               String callerRole) {
        assertAccess(requestedEmployeeId, callerEmployeeId, callerRole);

        Employee employee = employeeRepository.findById(requestedEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + requestedEmployeeId));

        int year = LocalDate.now().getYear();

        // Fixed: was using undefined "employeeId", and now returns List not Optional
        List<LeaveEntitlement> entitlements = leaveEntitlementRepository
                .findByEmployeeEmployeeIdAndYear(requestedEmployeeId, year);

        List<LeaveWarning> warnings =
                warningRepository.findByEmployeeEmployeeIdAndIsReadFalseOrderByWarningDateDesc(
                        requestedEmployeeId);

        LeaveDashboardResponse.LeaveBalanceDto sick      = buildBalanceDto(entitlements, "SICK");
        LeaveDashboardResponse.LeaveBalanceDto casual    = buildBalanceDto(entitlements, "CASUAL");
        LeaveDashboardResponse.LeaveBalanceDto maternity = buildBalanceDto(entitlements, "MATERNITY");

        BigDecimal carryTotal = sum(
                casual    != null ? casual.getAvailable()    : null,
                maternity != null ? maternity.getAvailable() : null
        );

        LeaveDashboardResponse response = new LeaveDashboardResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setSick(sick);
        response.setCasual(casual);
        response.setMaternity(maternity);
        response.setCarryForwardMeter(new LeaveDashboardResponse.CarryForwardMeterDto(carryTotal, 30));
        response.setWarnings(warnings.stream()
                .map(w -> new LeaveDashboardResponse.WarningDto(
                        w.getId(),
                        w.getWarningType().name(),
                        w.getMessage(),
                        w.getWarningDate().toString()))
                .toList());

        return response;
    }

    // ── Mark warning read ────────────────────────────────────

    @Transactional
    public void markWarningRead(Long warningId, Long callerEmployeeId, String callerRole) {
        LeaveWarning warning = warningRepository.findById(warningId)
                .orElseThrow(() -> new IllegalArgumentException("Warning not found: " + warningId));

        assertAccess(warning.getEmployee().getEmployeeId(),callerEmployeeId, callerRole);

        warning.setRead(true);
        warningRepository.save(warning);
    }

    // ── Team dashboard (manager/admin) ───────────────────────

    @Transactional(readOnly = true)
    public List<LeaveDashboardResponse> getTeamDashboard(Long managerId, String callerRole) {
        if (!"MANAGER".equals(callerRole) && !"ADMIN".equals(callerRole)) {
            throw new AccessDeniedException("Only managers and admins can view team dashboards");
        }

        List<Employee> team = employeeRepository.findByManagerEmployeeId(managerId);
        return team.stream()
                .map(emp -> getDashboard(emp.getEmployeeId(), managerId, callerRole))
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────

    private LeaveDashboardResponse.LeaveBalanceDto buildBalanceDto(List<LeaveEntitlement> entitlements,
                                                                   String typeName) {
        return entitlements.stream()
                .filter(e -> e.getLeaveType().getName().equals(typeName))
                .findFirst()
                .map(e -> new LeaveDashboardResponse.LeaveBalanceDto(
                        typeName,
                        e.getOpeningBalance(),
                        e.getAccruedThisYear(),
                        e.getUsedThisYear(),
                        e.getAvailableBalance(),
                        e.getLeaveType().isCarriesForward()))
                .orElse(null);  // null = leave type not applicable for this employee
    }

    private BigDecimal sum(BigDecimal a, BigDecimal b) {
        BigDecimal result = BigDecimal.ZERO;
        if (a != null) result = result.add(a);
        if (b != null) result = result.add(b);
        return result;
    }

    private void assertAccess(Long targetId, Long callerId, String role) {
        boolean isSelf    = targetId.equals(callerId);
        boolean isManager = "MANAGER".equals(role) || "ADMIN".equals(role);
        if (!isSelf && !isManager) {
            throw new AccessDeniedException(
                    "Employee " + callerId + " cannot view dashboard for " + targetId);
        }
    }
}