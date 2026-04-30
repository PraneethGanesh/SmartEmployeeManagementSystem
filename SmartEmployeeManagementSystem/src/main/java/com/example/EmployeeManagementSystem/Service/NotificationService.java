package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Notification;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final EmployeeRepo employeeRepo;

    public NotificationService(NotificationRepository notificationRepo,
                               EmployeeRepo employeeRepo) {
        this.notificationRepo = notificationRepo;
        this.employeeRepo = employeeRepo;
    }

    /** Create a notification for a specific employee */
    public void notify(Employee recipient, String message, String type) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setMessage(message);
        n.setType(type);
        notificationRepo.save(n);
    }

    /** Notify every employee in the system (e.g. new public holiday) */
    public void notifyAll(String message, String type) {
        List<Employee> all = employeeRepo.findAll();
        for (Employee e : all) {
            notify(e, message, type);
        }
    }

    public List<Notification> getForEmployee(Employee employee) {
        return notificationRepo.findByRecipientOrderByCreatedAtDesc(employee);
    }

    public long getUnreadCount(Employee employee) {
        return notificationRepo.countByRecipientAndIsReadFalse(employee);
    }

    public void markAllRead(Employee employee) {
        List<Notification> unread = notificationRepo
                .findByRecipientOrderByCreatedAtDesc(employee)
                .stream()
                .filter(n -> !n.isRead())
                .toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unread);
    }
}
