package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.*;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveWarningRepository;
import com.example.EmployeeManagementSystem.Repository.YearEndCarryForwardLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class YearEndCarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(YearEndCarryForwardService.class);
    private static final BigDecimal CAP = BigDecimal.valueOf(30);
    private static final Set<String> SUPPORTED_LEAVE_TYPES = Set.of("SICK", "CASUAL", "MATERNITY");

    private final EmployeeRepo employeeRepository;
    private final LeaveEntitlementRepository entitlementRepository;
    private final YearEndCarryForwardLogRepository logRepository;
    private final LeaveWarningRepository warningRepository;

    public YearEndCarryForwardService(
            EmployeeRepo employeeRepository,
            LeaveEntitlementRepository entitlementRepository,
            YearEndCarryForwardLogRepository logRepository,
            LeaveWarningRepository warningRepository) {
        this.employeeRepository = employeeRepository;
        this.entitlementRepository = entitlementRepository;
        this.logRepository = logRepository;
        this.warningRepository = warningRepository;
    }

    // ---------------------------------------------------------------
    // Main entry point — called by scheduler on Dec 31 at 22:00
    // ---------------------------------------------------------------
    @Transactional
    public void runYearEnd(int closingYear) {

        // Idempotency guard — never run twice for same year
        if (logRepository.existsByProcessedYear(closingYear)) {
            log.warn("Year-end carry-forward already processed for {}. Skipping.", closingYear);
            return;
        }

        List<Employee> activeEmployees = employeeRepository.findByStatus(Status.ACTIVE);
        log.info("Starting year-end carry-forward for {} — {} employees", closingYear, activeEmployees.size());

        for (Employee employee : activeEmployees) {
            processEmployeeYearEnd(employee, closingYear);
        }

        log.info("Year-end carry-forward complete for {}", closingYear);
    }

    // ---------------------------------------------------------------
    // Per-employee processing
    // ---------------------------------------------------------------
    private void processEmployeeYearEnd(Employee employee, int closingYear) {
        int nextYear = closingYear + 1;

        // Fetch all entitlements for the closing year
        List<LeaveEntitlement> entitlements =
                entitlementRepository.findByEmployeeEmployeeIdAndYear(employee.getEmployeeId(), closingYear);

        // Separate carry-forwardable from non-carry-forwardable
        List<LeaveEntitlement> carryForwardable = entitlements.stream()
                .filter(e -> SUPPORTED_LEAVE_TYPES.contains(e.getLeaveType().getName()))
                .filter(e -> e.getLeaveType().isCarriesForward())
                .collect(Collectors.toList());

        // Step 1 — compute raw closing balance for each type
        //           closing = opening + accrued - used
        Map<LeaveEntitlement, BigDecimal> rawClosing = carryForwardable.stream()
                .collect(Collectors.toMap(
                        e -> e,
                        e -> e.getOpeningBalance()
                                .add(e.getAccruedThisYear())
                                .subtract(e.getUsedThisYear())
                                .max(BigDecimal.ZERO)   // never negative
                ));

        // Step 2 — sum all carry-forwardable closing balances
        BigDecimal totalRaw = rawClosing.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 3 — apply cap proportionally if total exceeds 30
        Map<LeaveEntitlement, BigDecimal> cappedClosing = applyCap(rawClosing, totalRaw);

        // Step 4 — persist: update closing balance on current year row,
        //           create/update opening balance on next year row, write log
        for (LeaveEntitlement entitlement : carryForwardable) {
            BigDecimal raw    = rawClosing.get(entitlement);
            BigDecimal capped = cappedClosing.get(entitlement);
            BigDecimal wasted = raw.subtract(capped);

            // Update closing balance on the current year's row
            entitlement.setClosingBalance(capped);
            entitlementRepository.save(entitlement);

            // Write or update next year's opening balance
            LeaveEntitlement nextYearRow = entitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), entitlement.getLeaveType().getId(), nextYear)
                    .orElseGet(() -> buildNextYearRow(employee, entitlement.getLeaveType(), nextYear));

            nextYearRow.setOpeningBalance(capped);
            entitlementRepository.save(nextYearRow);

            // Write audit log
            writeLog(employee, closingYear, entitlement.getLeaveType(), raw, capped, wasted);

            // Issue DAY_WASTED warning if any days were lost
            if (wasted.compareTo(BigDecimal.ZERO) > 0) {
                issueWastedWarning(employee, wasted, capped);
                log.info("Employee {} lost {} day(s) of {} to cap at year end",
                        employee.getEmployeeId(), wasted, entitlement.getLeaveType().getName());
            }
        }

        // Step 5 — zero out sick leave closing balance (it never carries)
        entitlements.stream()
                .filter(e -> SUPPORTED_LEAVE_TYPES.contains(e.getLeaveType().getName()))
                .filter(e -> !e.getLeaveType().isCarriesForward())
                .forEach(e -> {
                    e.setClosingBalance(BigDecimal.ZERO);
                    entitlementRepository.save(e);
                });

        log.info("Year-end processed for employee {} — total raw={} capped={}",
                employee.getEmployeeId(), totalRaw, CAP.min(totalRaw));
    }

    // ---------------------------------------------------------------
    // Cap logic — distribute cap proportionally across leave types
    //
    // Example: casual=20, maternity=16 → total=36, over cap by 6
    //   casual  gets: 20/36 * 30 = 16.67
    //   maternity gets: 16/36 * 30 = 13.33
    //   total = 30 exactly
    // ---------------------------------------------------------------
    private Map<LeaveEntitlement, BigDecimal> applyCap(
            Map<LeaveEntitlement, BigDecimal> rawClosing,
            BigDecimal totalRaw) {

        if (totalRaw.compareTo(CAP) <= 0) {
            // Under cap — no adjustment needed
            return rawClosing;
        }

        return rawClosing.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            BigDecimal raw = entry.getValue();
                            // proportion = raw / totalRaw * CAP
                            return raw
                                    .divide(totalRaw, 10, RoundingMode.HALF_UP)
                                    .multiply(CAP)
                                    .setScale(2, RoundingMode.HALF_UP);
                        }
                ));
    }

    // ---------------------------------------------------------------
    // Build a blank next-year entitlement row
    // ---------------------------------------------------------------
    private LeaveEntitlement buildNextYearRow(Employee employee, LeaveType leaveType, int year) {
        LeaveEntitlement row = new LeaveEntitlement();
        row.setEmployee(employee);
        row.setLeaveType(leaveType);
        row.setYear(year);
        row.setOpeningBalance(BigDecimal.ZERO);
        row.setAccruedThisYear(BigDecimal.ZERO);
        row.setUsedThisYear(BigDecimal.ZERO);
        row.setClosingBalance(BigDecimal.ZERO);
        return entitlementRepository.save(row);
    }

    // ---------------------------------------------------------------
    // Write audit log entry
    // ---------------------------------------------------------------
    private void writeLog(Employee employee, int year, LeaveType leaveType,
                          BigDecimal raw, BigDecimal capped, BigDecimal wasted) {
        YearEndCarryForwardLog entry = new YearEndCarryForwardLog();
        entry.setEmployee(employee);
        entry.setProcessedYear(year);
        entry.setLeaveType(leaveType);
        entry.setClosingBalance(raw);
        entry.setCappedBalance(capped);
        entry.setDaysWasted(wasted);
        entry.setOpeningNextYear(capped);
        logRepository.save(entry);
    }

    // ---------------------------------------------------------------
    // Issue a DAY_WASTED warning on the dashboard
    // ---------------------------------------------------------------
    private void issueWastedWarning(Employee employee, BigDecimal wasted, BigDecimal newBalance) {
        LeaveWarning warning = new LeaveWarning();
        warning.setEmployee(employee);
        warning.setWarningType(LeaveWarning.WarningType.DAY_WASTED);
        warning.setCarryForwardBalance(newBalance);
        warning.setWarningDate(LocalDate.now());
        warning.setMessage(
                wasted.toPlainString() + " carry-forward day(s) were lost at year-end " +
                        "because your balance exceeded the 30-day cap. " +
                        "Your opening balance for next year is " + newBalance.toPlainString() + " days."
        );
        warningRepository.save(warning);
    }
}
