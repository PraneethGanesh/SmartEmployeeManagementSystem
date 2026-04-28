package com.example.EmployeeManagementSystem.Repository;


import com.example.EmployeeManagementSystem.Entity.YearEndCarryForwardLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YearEndCarryForwardLogRepository
        extends JpaRepository<YearEndCarryForwardLog, Long> {

    List<YearEndCarryForwardLog> findByEmployeeEmployeeIdAndProcessedYear(
            Long employeeId, Integer year
    );

    // Check if year-end has already been run for this year (idempotency guard)
    boolean existsByProcessedYear(Integer year);
}