package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairBillRepository extends JpaRepository<RepairBill,Long> {
    List<RepairBill> findByPaymentStatusNot(PaymentStatus paymentStatus);
}
