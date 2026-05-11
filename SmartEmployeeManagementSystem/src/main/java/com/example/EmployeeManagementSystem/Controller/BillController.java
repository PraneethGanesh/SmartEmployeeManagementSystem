package com.example.EmployeeManagementSystem.Controller;


import com.example.EmployeeManagementSystem.DTO.InvoiceDTO;
import com.example.EmployeeManagementSystem.DTO.RentalBillDTO;
import com.example.EmployeeManagementSystem.DTO.RepairBillDTO;
import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Service.BillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/repairBill/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceDTO payRepairBill(@PathVariable Long id) {
        return billService.generateRepairInvoice(id);
    }

    @PostMapping("/rentalBill/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceDTO payRentalBill(@PathVariable Long id) {
        return billService.generateRentalInvoice(id);
    }
}
