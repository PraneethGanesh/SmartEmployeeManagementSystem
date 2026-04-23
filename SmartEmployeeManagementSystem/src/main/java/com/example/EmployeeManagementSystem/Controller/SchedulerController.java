package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Scheduler.LeaveStatusScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Scheduler Operations
 */
@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    private final LeaveStatusScheduler leaveStatusScheduler;

    public SchedulerController(LeaveStatusScheduler leaveStatusScheduler) {
        this.leaveStatusScheduler = leaveStatusScheduler;
    }

    /**
     * Manual trigger for leave scheduler
     */
    @PostMapping("/run")
    public ResponseEntity<?> runScheduler() {
        try {
            leaveStatusScheduler.runManually();

            return ResponseEntity.ok().body(
                    Map.of(
                            "status", "success",
                            "message", "Scheduler executed successfully"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    )
            );
        }
    }
}