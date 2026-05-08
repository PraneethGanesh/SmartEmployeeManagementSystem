package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.VendorNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorNegotiationRepository extends JpaRepository<VendorNegotiation, Long> {
    List<VendorNegotiation> findByVendorIdOrderByUpdatedAtDesc(Long vendorId);
    List<VendorNegotiation> findAllByOrderByUpdatedAtDesc();
}
