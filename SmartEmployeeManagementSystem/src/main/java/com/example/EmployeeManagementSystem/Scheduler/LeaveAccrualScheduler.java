package com.example.EmployeeManagementSystem.Scheduler;

import com.example.EmployeeManagementSystem.Service.CarryForwardWarningService;
import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import com.example.EmployeeManagementSystem.Service.SickLeaveResetService;
import com.example.EmployeeManagementSystem.Service.YearEndCarryForwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LeaveAccrualScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualScheduler.class);

    private final LeaveAccrualService accrualService;
    private final SickLeaveResetService resetService;
    private final CarryForwardWarningService warningService;
    private final YearEndCarryForwardService yearEndService;

    public LeaveAccrualScheduler(LeaveAccrualService accrualService,
                                 SickLeaveResetService resetService,
                                 CarryForwardWarningService warningService,
                                 YearEndCarryForwardService yearEndService) {
        this.accrualService = accrualService;
        this.resetService = resetService;
        this.warningService = warningService;
        this.yearEndService = yearEndService;
    }

    // 1st of month — accrue then warn
    @Scheduled(cron = "0 5 0 1 * ?")
    public void runMonthlyAccrual() {
        LocalDate today = LocalDate.now();
        accrualService.runMonthlyAccrual(today);
        warningService.runWarningCheck(today);
    }

    // Last day of month 23:55 — sick leave reset
    @Scheduled(cron = "0 55 23 L * ?")
    public void runSickLeaveReset() {
        resetService.runMonthlyReset(LocalDate.now());
    }

    // Dec 31 at 22:00 — year-end carry-forward cap
    @Scheduled(cron = "0 0 22 31 12 ?")
    public void runYearEnd() {
        int closingYear = LocalDate.now().getYear();
        yearEndService.runYearEnd(closingYear);
    }
}