package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, Long entityId);
    List<OperationLog> findByOperationTypeOrderByOccurredAtDesc(String operationType);
    List<OperationLog> findAllByOrderByOccurredAtDesc();
}
