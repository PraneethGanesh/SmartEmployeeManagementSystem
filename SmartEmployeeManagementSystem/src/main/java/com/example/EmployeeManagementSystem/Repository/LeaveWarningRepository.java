package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.LeaveWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveWarningRepository extends JpaRepository<LeaveWarning, Long> {

    // All unread warnings for an employee — shown on dashboard
    List<LeaveWarning> findByEmployeeEmployeeIdAndIsReadFalseOrderByWarningDateDesc(Long employeeId);

    // All warnings for an employee (read + unread) — for history view
    List<LeaveWarning> findByEmployeeEmployeeIdOrderByWarningDateDesc(Long employeeId);

    // Prevent duplicate warnings for same type on same date
    boolean existsByEmployeeEmployeeIdAndWarningTypeAndWarningDate(
            Long employeeId, LeaveWarning.WarningType warningType, LocalDate date
    );
}