package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.InvoiceDTO;
import com.example.EmployeeManagementSystem.Service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // Vendor fetches their pending invoices (SENT only)
    @GetMapping("/vendor/me")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public List<InvoiceDTO> getMyInvoices(Authentication authentication) {
        return invoiceService.getPendingInvoicesForCurrentVendor(authentication);
    }

    // Vendor clicks "Mark as Received" — removes from their view
    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('TECH_VENDOR')")
    public ResponseEntity<String> acknowledgeInvoice(@PathVariable Long id,Authentication authentication) {
        return invoiceService.acknowledgeInvoice(id,authentication);
        // This sets status = ACKNOWLEDGED, marks bill PAID, sets seenAt
    }

    // Admin can see all invoices
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceDTO> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }
}