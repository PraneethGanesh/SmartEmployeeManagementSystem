package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.RentalBillDTO;
import com.example.EmployeeManagementSystem.DTO.RepairBillDTO;
import com.example.EmployeeManagementSystem.Entity.RentalBill;
import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import com.example.EmployeeManagementSystem.Repository.RentalBillRepository;
import com.example.EmployeeManagementSystem.Repository.RepairBillRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BillService {
    private final RepairBillRepository repairBillRepository;
    private final RentalBillRepository rentalBillRepository;

    public BillService(RepairBillRepository repairBillRepository, RentalBillRepository rentalBillRepository) {
        this.repairBillRepository = repairBillRepository;
        this.rentalBillRepository = rentalBillRepository;
    }

    public List<RepairBillDTO> getRepairBills(){
       List<RepairBill> repairBills=repairBillRepository.findByPaymentStatusNotAndRepairCostGreaterThan(
               PaymentStatus.PAID,
               BigDecimal.ZERO
       );
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

    public List<RentalBillDTO> getRentalBill(){
        List<RentalBill> rentalBills=rentalBillRepository.findByPaymentStatusNotAndAmountGreaterThan(
                PaymentStatus.PAID,
                BigDecimal.ZERO
        );
        return rentalBills.stream().map(this::toRentalBillDTO).toList();
    }

    public RentalBillDTO toRentalBillDTO(RentalBill rentalBill){
        RentalBillDTO rentalBillDTO = new RentalBillDTO();
        rentalBillDTO.setId(rentalBill.getId());
        rentalBillDTO.setAmount(rentalBill.getAmount());
        rentalBillDTO.setBillDate(rentalBill.getBillDate());
        rentalBillDTO.setDueDate(rentalBill.getDueDate());
        rentalBillDTO.setVendorName(rentalBill.getVendor().getName());
        rentalBillDTO.setPaymentStatus(rentalBill.getPaymentStatus().name());
        return rentalBillDTO;
    }
}
