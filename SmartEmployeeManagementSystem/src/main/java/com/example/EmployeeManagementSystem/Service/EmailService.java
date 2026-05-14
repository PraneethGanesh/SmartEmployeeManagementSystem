package com.example.EmployeeManagementSystem.Service;


import com.example.EmployeeManagementSystem.Entity.Employee;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendWelcomeEmail(Employee employee, String rawPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(employee.getEmail());
            helper.setSubject("Welcome — your account is ready");

            Context ctx = new Context();
            ctx.setVariable("name", employee.getName());
            ctx.setVariable("email", employee.getEmail());
            ctx.setVariable("tempPassword", rawPassword);
            ctx.setVariable("resetLink", baseUrl + "/employee/reset-password?token=" + employee.getResetToken());

            helper.setText(templateEngine.process("emails/welcome-employee", ctx), true);
            mailSender.send(message);

            log.info("Welcome email sent to {}", employee.getEmail());
        } catch (MessagingException e) {
            throw new RuntimeException("Email send failed: " + employee.getEmail(), e);
        }
    }
}