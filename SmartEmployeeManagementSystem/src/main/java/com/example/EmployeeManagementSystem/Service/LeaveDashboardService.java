package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.LeaveDashboardResponse;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Entity.LeaveWarning;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveWarningRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveDashboardService {

    private static final Set<String> SUPPORTED_LEAVE_TYPES = Set.of("SICK", "CASUAL", "MATERNITY");

    private final EmployeeRepo employeeRepository;
    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveWarningRepository warningRepository;

    public LeaveDashboardService(EmployeeRepo employeeRepository,
                                 LeaveEntitlementRepository leaveEntitlementRepository,
                                 LeaveTypeRepository leaveTypeRepository,
                                 LeaveWarningRepository warningRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveEntitlementRepository = leaveEntitlementRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.warningRepository = warningRepository;
    }

    @Transactional
    public LeaveDashboardResponse getDashboard(Long requestedEmployeeId,
                                               Long callerEmployeeId,
                                               String callerRole) {
        assertAccess(requestedEmployeeId, callerEmployeeId, callerRole);

        Employee employee = employeeRepository.findById(requestedEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + requestedEmployeeId));

        int year = LocalDate.now().getYear();

        ensureCurrentYearEntitlements(employee, year);
        List<LeaveEntitlement> entitlements =
                leaveEntitlementRepository.findByEmployeeEmployeeIdAndYear(requestedEmployeeId, year);

        List<LeaveWarning> warnings =
                warningRepository.findByEmployeeEmployeeIdAndIsReadFalseOrderByWarningDateDesc(
                        requestedEmployeeId);

        LeaveDashboardResponse.LeaveBalanceDto sick      = buildBalanceDto(entitlements, "SICK");
        LeaveDashboardResponse.LeaveBalanceDto casual    = buildBalanceDto(entitlements, "CASUAL");
        LeaveDashboardResponse.LeaveBalanceDto maternity = buildBalanceDto(entitlements, "MATERNITY");

        Map<String, LeaveDashboardResponse.LeaveBalanceDto> leaveBalances = buildBalanceMap(entitlements);
        BigDecimal carryTotal = entitlements.stream()
                .filter(e -> isSupportedLeaveType(e.getLeaveType()))
                .filter(e -> e.getLeaveType().isCarriesForward())
                .map(LeaveEntitlement::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LeaveDashboardResponse response = new LeaveDashboardResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setSick(sick);
        response.setCasual(casual);
        response.setMaternity(maternity);
        response.setLeaveBalances(leaveBalances);
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

    @Transactional
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

    private void ensureCurrentYearEntitlements(Employee employee, int year) {
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        for (LeaveType leaveType : leaveTypes) {
            if (!isSupportedLeaveType(leaveType)) {
                continue;
            }
            if (!isEligible(employee, leaveType)) {
                continue;
            }

            leaveEntitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year)
                    .orElseGet(() -> createEntitlement(employee, leaveType, year));
        }
    }

    private boolean isEligible(Employee employee, LeaveType leaveType) {
        if (!leaveType.isGenderRestricted()) {
            return true;
        }

        Gender restriction = leaveType.getGenderRestriction();
        return restriction == null || restriction == employee.getGender();
    }

    private LeaveEntitlement createEntitlement(Employee employee, LeaveType leaveType, int year) {
        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(leaveType);
        entitlement.setYear(year);

        if (leaveType.isCarriesForward()) {
            BigDecimal lastYearClosing = leaveEntitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year - 1)
                    .map(LeaveEntitlement::getClosingBalance)
                    .orElse(BigDecimal.ZERO);
            entitlement.setOpeningBalance(lastYearClosing);
        }

        return leaveEntitlementRepository.save(entitlement);
    }

    private Map<String, LeaveDashboardResponse.LeaveBalanceDto> buildBalanceMap(
            List<LeaveEntitlement> entitlements) {
        Map<String, LeaveDashboardResponse.LeaveBalanceDto> balances = new LinkedHashMap<>();
        for (LeaveEntitlement entitlement : entitlements) {
            String typeName = entitlement.getLeaveType().getName();
            if (!SUPPORTED_LEAVE_TYPES.contains(typeName)) {
                continue;
            }
            balances.put(typeName, toBalanceDto(entitlement, typeName));
        }
        return balances;
    }

    private LeaveDashboardResponse.LeaveBalanceDto buildBalanceDto(List<LeaveEntitlement> entitlements,
                                                                   String typeName) {
        return entitlements.stream()
                .filter(e -> typeName.equalsIgnoreCase(e.getLeaveType().getName()))
                .findFirst()
                .map(e -> toBalanceDto(e, typeName))
                .orElse(null);  // null = leave type not applicable for this employee
    }

    private LeaveDashboardResponse.LeaveBalanceDto toBalanceDto(LeaveEntitlement entitlement,
                                                                String typeName) {
        return new LeaveDashboardResponse.LeaveBalanceDto(
                typeName,
                entitlement.getOpeningBalance(),
                entitlement.getAccruedThisYear(),
                entitlement.getUsedThisYear(),
                entitlement.getAvailableBalance(),
                entitlement.getLeaveType().isCarriesForward());
    }

    private boolean isSupportedLeaveType(LeaveType leaveType) {
        return leaveType != null && SUPPORTED_LEAVE_TYPES.contains(leaveType.getName());
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
