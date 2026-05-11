package com.example.EmployeeManagementSystem.Controller;


import com.example.EmployeeManagementSystem.DTO.RentalBillDTO;
import com.example.EmployeeManagementSystem.DTO.RepairBillDTO;
import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Service.BillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/repairBill")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RepairBillDTO> getAllRepairBill(){
        return billService.getRepairBills();
    }

    @GetMapping("/rentalBill")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RentalBillDTO> getAllRentalBill(){
        return billService.getRentalBill();
    }
}
