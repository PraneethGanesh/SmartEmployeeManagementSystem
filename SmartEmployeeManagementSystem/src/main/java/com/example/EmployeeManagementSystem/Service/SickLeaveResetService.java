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

        // Find all leave types that reset monthly (only SICK in your design)
        List<LeaveType> resetTypes = leaveTypeRepository.findByMonthlyResetTrue();

        for (LeaveType leaveType : resetTypes) {
            List<LeaveEntitlement> entitlements =
                    entitlementRepository.findByLeaveTypeIdAndYear(leaveType.getId(), year);

            for (LeaveEntitlement entitlement : entitlements) {

                BigDecimal available = entitlement.getAvailableBalance();

                if (available.compareTo(BigDecimal.ZERO) > 0) {
                    // Wipe unused days — move accrued back to zero effect
                    // We do this by setting accrued = used (so available = 0)
                    // This preserves the audit trail of what was used
                    entitlement.setAccruedThisYear(entitlement.getUsedThisYear());

                    log.info("Sick leave reset for employee {} — {} unused day(s) expired on {}",
                            entitlement.getEmployee().getEmployeeId(), available, resetDate);
                } else {
                    log.debug("Sick leave reset for employee {} — nothing to expire",
                            entitlement.getEmployee().getEmployeeId());
                }

                entitlementRepository.save(entitlement);
            }
        }

        log.info("Sick leave monthly reset complete for {}", resetDate);
    }
}