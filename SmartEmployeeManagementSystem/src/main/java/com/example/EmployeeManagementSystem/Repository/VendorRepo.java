package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepo extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    boolean existsByEmail(String email);
}