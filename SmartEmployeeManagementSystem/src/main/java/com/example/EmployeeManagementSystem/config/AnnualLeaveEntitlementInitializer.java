package com.example.EmployeeManagementSystem.config;

import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(2)
public class AnnualLeaveEntitlementInitializer implements CommandLineRunner {

    private final LeaveAccrualService leaveAccrualService;

    public AnnualLeaveEntitlementInitializer(LeaveAccrualService leaveAccrualService) {
        this.leaveAccrualService = leaveAccrualService;
    }

    @Override
    public void run(String... args) {
        leaveAccrualService.syncAnnualEntitlements(LocalDate.now());
    }
}
