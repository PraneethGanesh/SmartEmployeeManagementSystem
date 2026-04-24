package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.RefreshToken;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Needed so EmployeeService can clean up tokens before deleting an employee
    void deleteByEmployee(Employee employee);

    // Needed so VendorService can clean up tokens before deleting a vendor
    void deleteByVendor(Vendor vendor);
}