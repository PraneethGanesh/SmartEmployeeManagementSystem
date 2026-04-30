package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Notification;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeRepo employeeRepo;

    public NotificationController(NotificationService notificationService,
                                  EmployeeRepo employeeRepo) {
        this.notificationService = notificationService;
        this.employeeRepo = employeeRepo;
    }

    /** GET /notifications — all notifications for logged-in user */
    @GetMapping
    public ResponseEntity<List<Notification>> getAll(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        return ResponseEntity.ok(notificationService.getForEmployee(employee));
    }

    /** GET /notifications/unread-count — for the badge number */
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        return ResponseEntity.ok(Map.of("count",
                notificationService.getUnreadCount(employee)));
    }

    /** POST /notifications/mark-read — mark all as read when bell is opened */
    @PostMapping("/mark-read")
    public ResponseEntity<?> markRead(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        notificationService.markAllRead(employee);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    private Employee getEmployee(Authentication authentication) {
        return employeeRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
}
