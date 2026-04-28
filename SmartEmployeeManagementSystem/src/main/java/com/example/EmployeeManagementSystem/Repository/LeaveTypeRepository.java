package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Integer> {
    Optional<LeaveType> findByName(String name);
}