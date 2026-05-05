package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Service.CarryForwardWarningService;
import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import com.example.EmployeeManagementSystem.Service.SickLeaveResetService;
import com.example.EmployeeManagementSystem.Service.YearEndCarryForwardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/scheduler")
@PreAuthorize("hasRole('ADMIN')")
public class SchedulerTestController {

    private final LeaveAccrualService accrualService;
    private final SickLeaveResetService resetService;
    private final CarryForwardWarningService warningService;
    private final YearEndCarryForwardService yearEndService;

    public SchedulerTestController(LeaveAccrualService accrualService,
                                   SickLeaveResetService resetService,
                                   CarryForwardWarningService warningService,
                                   YearEndCarryForwardService yearEndService) {
        this.accrualService = accrualService;
        this.resetService = resetService;
        this.warningService = warningService;
        this.yearEndService = yearEndService;
    }

    // Trigger monthly accrual as if today were the 1st of the month
    @PostMapping("/run-accrual")
    public ResponseEntity<String> runAccrual(
            @RequestParam(required = false) String date) {
        LocalDate target = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        accrualService.runMonthlyAccrual(target);
        warningService.runWarningCheck(target);
        return ResponseEntity.ok("Accrual ran for date: " + target);
    }

    // Trigger sick leave reset as if today were end of month
    @PostMapping("/run-sick-reset")
    public ResponseEntity<String> runSickReset(
            @RequestParam(required = false) String date) {
        LocalDate target = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        resetService.runMonthlyReset(target);
        return ResponseEntity.ok("Sick leave reset ran for date: " + target);
    }

    // Trigger year-end carry forward
    @PostMapping("/run-year-end")
    public ResponseEntity<String> runYearEnd(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        yearEndService.runYearEnd(targetYear);
        return ResponseEntity.ok("Year-end ran for year: " + targetYear);
    }
}