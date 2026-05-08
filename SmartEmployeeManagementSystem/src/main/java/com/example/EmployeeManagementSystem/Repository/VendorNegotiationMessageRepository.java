package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.VendorNegotiationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorNegotiationMessageRepository extends JpaRepository<VendorNegotiationMessage, Long> {
    List<VendorNegotiationMessage> findByNegotiationIdOrderByCreatedAtAsc(Long negotiationId);
}
