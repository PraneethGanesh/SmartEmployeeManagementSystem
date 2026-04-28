package com.example.EmployeeManagementSystem.Scheduler;

import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LeaveAccrualScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualScheduler.class);
    private final LeaveAccrualService accrualService;

    public LeaveAccrualScheduler(LeaveAccrualService accrualService) {
        this.accrualService = accrualService;
    }

    // Runs at 00:05 on the 1st of every month
    // "0 5 0 1 * ?" = second=0, minute=5, hour=0, dayOfMonth=1, every month
    @Scheduled(cron = "0 5 0 1 * ?")
    public void runMonthlyAccrual() {
        LocalDate today = LocalDate.now();
        log.info("Accrual scheduler triggered for {}", today);
        accrualService.runMonthlyAccrual(today);
    }
}