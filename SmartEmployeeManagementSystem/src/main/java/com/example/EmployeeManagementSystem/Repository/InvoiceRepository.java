package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.*;
import com.example.EmployeeManagementSystem.Enum.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice,Long> {
    List<Invoice> findByVendorAndStatus(Vendor current, InvoiceStatus invoiceStatus);

    boolean existsByRepairBill(RepairBill repairBill);

    boolean existsByRentalBill(RentalBill rentalBill);

    List<Invoice> findByRepairBillDeviceId(Long id);
}
