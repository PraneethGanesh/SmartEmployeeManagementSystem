package com.example.EmployeeManagementSystem.Scheduler;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LeaveStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeaveStatusScheduler.class);

    private final LeaveRequestRepo leaveRequestRepo;
    private final EmployeeRepo employeeRepo;

    public LeaveStatusScheduler(LeaveRequestRepo leaveRequestRepo,
                                EmployeeRepo employeeRepo) {
        this.leaveRequestRepo = leaveRequestRepo;
        this.employeeRepo = employeeRepo;
    }

    /**
     * Runs every day at midnight UTC
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void updateEmployeeLeaveStatus() {

        log.info("=== Starting Timezone-Aware Leave Scheduler ===");

        List<LeaveRequest> leaves =
                leaveRequestRepo.findApprovedLeavesWithEmployee(LeaveStatus.APPROVED);

        Map<Long, List<LeaveRequest>> employeeLeavesMap = leaves.stream()
                .collect(Collectors.groupingBy(lr -> lr.getEmployee().getEmployeeId()));

        int onLeaveCount = 0;
        int activeCount = 0;

        for (Map.Entry<Long, List<LeaveRequest>> entry : employeeLeavesMap.entrySet()) {

            Employee employee = entry.getValue().get(0).getEmployee();

            ZoneId zone = ZoneId.of(
                    employee.getTimezone() != null ? employee.getTimezone() : "UTC"
            );

            LocalDate today = LocalDate.now(zone);

            boolean isOnLeaveToday = entry.getValue().stream().anyMatch(leave ->
                    !today.isBefore(leave.getStartDate()) &&
                            !today.isAfter(leave.getEndDate())
            );

            // ✅ CASE 1: Should be ON_LEAVE
            if (isOnLeaveToday && employee.getStatus() != Status.ON_LEAVE) {

                employee.setStatus(Status.ON_LEAVE);
                employeeRepo.save(employee);
                onLeaveCount++;

                log.info("Employee {} ({}) marked ON_LEAVE | TZ: {}",
                        employee.getEmployeeId(),
                        employee.getName(),
                        zone);
            }

            // ✅ CASE 2: Should be ACTIVE
            if (!isOnLeaveToday && employee.getStatus() == Status.ON_LEAVE) {

                employee.setStatus(Status.ACTIVE);
                employeeRepo.save(employee);
                activeCount++;

                log.info("Employee {} ({}) marked ACTIVE | TZ: {}",
                        employee.getEmployeeId(),
                        employee.getName(),
                        zone);
            }
        }

        log.info("=== Scheduler Completed ===");
        log.info("ON_LEAVE updated: {}", onLeaveCount);
        log.info("ACTIVE updated: {}", activeCount);
    }

    /**
     * Manual trigger (for testing)
     */
    public void runManually() {
        log.info("Manual scheduler trigger started...");
        updateEmployeeLeaveStatus();
    }
}