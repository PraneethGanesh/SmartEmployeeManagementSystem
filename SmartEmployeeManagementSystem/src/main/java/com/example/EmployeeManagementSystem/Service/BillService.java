package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.RepairBillDTO;
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

    public List<RepairBillDTO> getRepairBills(){
       List<RepairBill> repairBills=repairBillRepository.findByPaymentStatusNot(PaymentStatus.PAID);
      return repairBills.stream().map(this::toRepairBillDTO).toList();
    }

    public RepairBillDTO toRepairBillDTO(RepairBill repairBill){
        RepairBillDTO repairBillDTO=new RepairBillDTO();
        repairBillDTO.setBillId(repairBill.getId());
        repairBillDTO.setDeviceName(repairBill.getDevice().getDeviceName());
        repairBillDTO.setRepairedBy(repairBill.getRepairLog().getRepairedBy());
        repairBillDTO.setRepairedDate(repairBill.getRepairLog().getRepairDate());
        repairBillDTO.setRepairCost(repairBill.getRepairCost());
        repairBillDTO.setGeneartedOn(repairBill.getGeneratedDate());
        repairBillDTO.setStatus(repairBill.getPaymentStatus().name());
        return repairBillDTO;
    }
}
