package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.InvoiceDTO;
import com.example.EmployeeManagementSystem.Entity.Invoice;
import com.example.EmployeeManagementSystem.Entity.RentalBill;
import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.InvoiceStatus;
import com.example.EmployeeManagementSystem.Enum.InvoiceType;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.InvoiceRepository;
import com.example.EmployeeManagementSystem.Repository.RentalBillRepository;
import com.example.EmployeeManagementSystem.Repository.RepairBillRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public class InvoiceService {
    private final VendorRepo vendorRepo;
    private final InvoiceRepository invoiceRepository;
    private final RentalBillRepository rentalBillRepository;
    private final RepairBillRepository repairBillRepository;


    public InvoiceService(VendorRepo vendorRepo, InvoiceRepository invoiceRepository, RentalBillRepository rentalBillRepository, RepairBillRepository repairBillRepository) {
        this.vendorRepo = vendorRepo;
        this.invoiceRepository = invoiceRepository;
        this.rentalBillRepository = rentalBillRepository;
        this.repairBillRepository = repairBillRepository;
    }


    public List<InvoiceDTO> getPendingInvoicesForCurrentVendor(Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Vendor current = vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("vendor :"+email+" Not Found")
        );
        return invoiceRepository
                .findByVendorAndStatus(current, InvoiceStatus.SENT)
                .stream().map(this::toInvoiceDTO).toList();
    }

    private InvoiceDTO toInvoiceDTO(Invoice invoice) {
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setId(invoice.getId());
        invoiceDTO.setInvoiceType(invoice.getInvoiceType().name());
        invoiceDTO.setVendorName(invoice.getVendor().getName());
        invoiceDTO.setAmount(invoice.getAmount());
        invoiceDTO.setIssuedDate(invoice.getIssuedDate());
        invoiceDTO.setStatus(invoice.getStatus().name());
        invoiceDTO.setSeenAt(invoice.getSeenAt());
        return invoiceDTO;
    }

    public List<InvoiceDTO> getAllInvoices() {
        List<Invoice> invoices=invoiceRepository.findAll();
        return invoices.stream().map(invoice -> toInvoiceDTO(invoice)).toList();
    }

    public ResponseEntity<String> acknowledgeInvoice(Long id,Authentication authentication) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Security: ensure it belongs to current vendor
        String email=AuthUtil.extractEmail(authentication);
        Vendor current = vendorRepo.findByEmail(email).orElseThrow(
                ()->new VendorNotFoundException("vendor: "+email+" Not Found")
        );
        if (!invoice.getVendor().getId().equals(current.getId()))
            throw new AccessDeniedException("Not your invoice");

        invoice.setStatus(InvoiceStatus.ACKNOWLEDGED);
        invoice.setSeenAt(LocalDate.now());

        // Mark the underlying bill as PAID
        if (invoice.getInvoiceType() == InvoiceType.REPAIR && invoice.getRepairBill() != null) {
            RepairBill bill = invoice.getRepairBill();
            bill.setPaymentStatus(PaymentStatus.ACKNOWLEDGED);
            repairBillRepository.save(bill);
        } else if (invoice.getInvoiceType() == InvoiceType.RENTAL && invoice.getRentalBill() != null) {
            RentalBill bill = invoice.getRentalBill();
            bill.setPaymentStatus(PaymentStatus.ACKNOWLEDGED);
            rentalBillRepository.save(bill);
        }

        invoiceRepository.save(invoice);
        return ResponseEntity.ok("Invoice acknowledged");
    }
}
