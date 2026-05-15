package com.example.EmployeeManagementSystem.Listeners;

import com.example.EmployeeManagementSystem.Event.EmployeeCreatedEvent;
import com.example.EmployeeManagementSystem.Service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    @Async
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        try {
            emailService.sendWelcomeEmail(event.getEmployee(), event.getPassword());
        } catch (Exception e) {
            // Don't let email failure roll back employee creation
            log.error("Failed to send welcome email to {}: {}",
                    event.getEmployee().getEmail(), e.getMessage());
        }
    }
}
