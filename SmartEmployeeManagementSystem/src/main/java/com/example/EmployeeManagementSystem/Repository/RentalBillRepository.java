package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.RentalBill;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface RentalBillRepository extends JpaRepository<RentalBill,Long> {
    List<RentalBill> findByPaymentStatusNotAndAmountGreaterThan(PaymentStatus paymentStatus, BigDecimal zero);
}
