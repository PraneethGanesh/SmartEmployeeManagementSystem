package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveWarning;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveWarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CarryForwardWarningService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardWarningService.class);

    private static final BigDecimal CAP          = BigDecimal.valueOf(30);
    private static final BigDecimal WARN_AT_29   = BigDecimal.valueOf(29);
    private static final BigDecimal WARN_AT_28   = BigDecimal.valueOf(28);

    private final EmployeeRepo employeeRepository;
    private final LeaveEntitlementRepository entitlementRepository;
    private final LeaveWarningRepository warningRepository;

    public CarryForwardWarningService(EmployeeRepo employeeRepository,
                                      LeaveEntitlementRepository entitlementRepository,
                                      LeaveWarningRepository warningRepository) {
        this.employeeRepository = employeeRepository;
        this.entitlementRepository = entitlementRepository;
        this.warningRepository = warningRepository;
    }

    @Transactional
    public void runWarningCheck(LocalDate checkDate) {
        List<Employee> activeEmployees = employeeRepository.findByStatus(Status.ACTIVE);

        for (Employee employee : activeEmployees) {
            BigDecimal carryForwardTotal = calculateCarryForwardBalance(employee, checkDate);
            evaluateAndWarn(employee, carryForwardTotal, checkDate);
        }

        log.info("Warning check complete for {} — {} employees evaluated",
                checkDate, activeEmployees.size());
    }

    // ---------------------------------------------------------------
    // Sum available balance across all carry-forwardable types
    // for the current year (this is what will carry forward if unused)
    // ---------------------------------------------------------------
    private BigDecimal calculateCarryForwardBalance(Employee employee, LocalDate date) {
        int year = date.getYear();

        List<LeaveEntitlement> entitlements =
                entitlementRepository.findByEmployeeEmployeeIdAndYear(employee.getEmployeeId(), year);

        return entitlements.stream()
                .filter(e -> e.getLeaveType().isCarriesForward())
                .map(LeaveEntitlement::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------------------------------------------------------------
    // Decide which warning to issue based on carry-forward total
    // ---------------------------------------------------------------
    private void evaluateAndWarn(Employee employee, BigDecimal balance, LocalDate date) {
        int cmp30 = balance.compareTo(CAP);
        int cmp29 = balance.compareTo(WARN_AT_29);
        int cmp28 = balance.compareTo(WARN_AT_28);

        if (cmp30 > 0) {
            // Balance EXCEEDS cap — a day was wasted (cap enforcement happens in Phase 5,
            // but we flag it here so the warning appears immediately after accrual)
            issueWarning(employee, LeaveWarning.WarningType.DAY_WASTED, balance, date,
                    "A carry-forward day was wasted this month because your balance exceeded the 30-day cap.");

        } else if (cmp30 == 0) {
            issueWarning(employee, LeaveWarning.WarningType.CAP_REACHED, balance, date,
                    "Carry-forward cap reached (30 days). Next month's unused leave will be wasted.");

        } else if (cmp29 >= 0) {
            issueWarning(employee, LeaveWarning.WarningType.APPROACHING_29, balance, date,
                    "You have " + balance.toPlainString() + " carry-forward days. "
                            + "1 month until cap. Use leave before it stops accumulating.");

        } else if (cmp28 >= 0) {
            issueWarning(employee, LeaveWarning.WarningType.APPROACHING_28, balance, date,
                    "You have " + balance.toPlainString() + " carry-forward days. "
                            + "2 months until cap (30). Plan to use leave soon.");
        }
        // Below 28 — no warning needed
    }

    // ---------------------------------------------------------------
    // Persist warning — skip if same type already issued today
    // ---------------------------------------------------------------
    private void issueWarning(Employee employee, LeaveWarning.WarningType type,
                              BigDecimal balance, LocalDate date, String message) {

        boolean alreadyIssued = warningRepository
                .existsByEmployeeEmployeeIdAndWarningTypeAndWarningDate(employee.getEmployeeId(), type, date);

        if (alreadyIssued) {
            log.debug("Warning {} already issued for employee {} on {}", type, employee.getEmployeeId(), date);
            return;
        }

        LeaveWarning warning = new LeaveWarning();
        warning.setEmployee(employee);
        warning.setWarningType(type);
        warning.setMessage(message);
        warning.setCarryForwardBalance(balance);
        warning.setWarningDate(date);

        warningRepository.save(warning);

        log.info("Warning issued — employee={} type={} balance={} date={}",
                employee.getEmployeeId(), type, balance, date);
    }
}