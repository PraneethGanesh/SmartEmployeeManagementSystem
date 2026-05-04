package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Scheduler.LeaveStatusScheduler;
import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import com.example.EmployeeManagementSystem.Service.SickLeaveResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * Controller for Scheduler Operations
 */
@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    private final LeaveStatusScheduler leaveStatusScheduler;
    private final SickLeaveResetService sickLeaveResetService;   // FIX: was missing
    private final LeaveAccrualService leaveAccrualService;        // FIX: was missing

    public SchedulerController(LeaveStatusScheduler leaveStatusScheduler,
                               SickLeaveResetService sickLeaveResetService,
                               LeaveAccrualService leaveAccrualService) {
        this.leaveStatusScheduler = leaveStatusScheduler;
        this.sickLeaveResetService = sickLeaveResetService;
        this.leaveAccrualService = leaveAccrualService;
    }

    /**
     * Manual trigger for leave status scheduler
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCHEDULER_TRIGGER')")
    public ResponseEntity<?> runScheduler() {
        try {
            leaveStatusScheduler.runManually();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Leave status scheduler executed successfully"
            ));
        } catch (Exception e) {
            log.error("Leave status scheduler failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * FIX: Manual trigger for sick leave reset (missing from original).
     * Useful for recovery and testing without waiting for the end-of-month cron.
     */
    @PostMapping("/sick-leave-reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runSickLeaveReset() {
        try {
            LocalDate lastDayOfMonth = YearMonth.now().atEndOfMonth();
            sickLeaveResetService.runMonthlyReset(lastDayOfMonth);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Sick leave reset executed for " + lastDayOfMonth
            ));
        } catch (Exception e) {
            log.error("Sick leave reset failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * FIX: Manual trigger for monthly leave accrual (missing from original).
     * Useful for onboarding new employees or recovering from a missed cron run.
     */
    @PostMapping("/accrue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runMonthlyAccrual() {
        try {
            LocalDate today = LocalDate.now();
            leaveAccrualService.runMonthlyAccrual(today);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Monthly leave accrual executed for " + today
            ));
        } catch (Exception e) {
            log.error("Monthly accrual failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}