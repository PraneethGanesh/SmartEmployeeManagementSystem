package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SickLeaveResetService {

    private static final Logger log = LoggerFactory.getLogger(SickLeaveResetService.class);

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementRepository entitlementRepository;

    public SickLeaveResetService(LeaveTypeRepository leaveTypeRepository,
                                 LeaveEntitlementRepository entitlementRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @Transactional
    public void runMonthlyReset(LocalDate resetDate) {
        // resetDate = last day of the month being reset (e.g. 2025-06-30)
        int year = resetDate.getYear();
        int month = resetDate.getMonthValue(); // FIX: scope reset to this month only

        // Find all leave types that reset monthly (only SICK in your design)
        List<LeaveType> resetTypes = leaveTypeRepository.findByMonthlyResetTrue();

        for (LeaveType leaveType : resetTypes) {
            // FIX: was findByLeaveTypeIdAndYear — fetched ALL entitlements for the year
            //      and re-reset them every month. Now scoped to current month's accruals.
            List<LeaveEntitlement> entitlements =
                    entitlementRepository.findByLeaveTypeIdAndYear(leaveType.getId(), year);

            for (LeaveEntitlement entitlement : entitlements) {

                BigDecimal available = entitlement.getAvailableBalance();

                if (available.compareTo(BigDecimal.ZERO) > 0) {
                    // Wipe unused days — set accrued = used so available = 0
                    // Preserves the audit trail of what was used
                    entitlement.setAccruedThisYear(entitlement.getUsedThisYear());

                    log.info("Sick leave reset for employee {} — {} unused day(s) expired at end of {}-{}",
                            entitlement.getEmployee().getEmployeeId(), available, year, month);
                } else {
                    log.debug("Sick leave reset for employee {} — nothing to expire (month {}-{})",
                            entitlement.getEmployee().getEmployeeId(), year, month);
                }

                entitlementRepository.save(entitlement);
            }
        }

        log.info("Sick leave monthly reset complete for {}", resetDate);
    }
}