package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface RepairBillRepository extends JpaRepository<RepairBill,Long> {
    List<RepairBill> findByPaymentStatusNotAndRepairCostGreaterThan(
            PaymentStatus paymentStatus,
            BigDecimal repairCost
    );

    List<RepairBill> findByDeviceId(Long deviceId);
}
