package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import com.example.EmployeeManagementSystem.Repository.RepairBillRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillService {
    private final RepairBillRepository repairBillRepository;

    public BillService(RepairBillRepository repairBillRepository) {
        this.repairBillRepository = repairBillRepository;
    }

    public List<RepairBill> getRepairBills(){
       return repairBillRepository.findByPaymentStatusNot(PaymentStatus.PAID);
    }
}
