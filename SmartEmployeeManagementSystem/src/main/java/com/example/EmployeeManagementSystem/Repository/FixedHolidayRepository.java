package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.FixedHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FixedHolidayRepository extends JpaRepository<FixedHoliday,Long> {
    List<FixedHoliday> findByYear(int year);
    List<FixedHoliday> findByDateBetween(LocalDate start, LocalDate end);
}
