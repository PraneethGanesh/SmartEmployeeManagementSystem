package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.InvoiceDTO;
import com.example.EmployeeManagementSystem.DTO.RentalBillDTO;
import com.example.EmployeeManagementSystem.DTO.RepairBillDTO;
import com.example.EmployeeManagementSystem.Entity.Invoice;
import com.example.EmployeeManagementSystem.Entity.RentalBill;
import com.example.EmployeeManagementSystem.Entity.RepairBill;
import com.example.EmployeeManagementSystem.Enum.InvoiceStatus;
import com.example.EmployeeManagementSystem.Enum.InvoiceType;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import com.example.EmployeeManagementSystem.Exception.RentalBillNotFoundException;
import com.example.EmployeeManagementSystem.Exception.RepairBillNotFoundException;
import com.example.EmployeeManagementSystem.Repository.InvoiceRepository;
import com.example.EmployeeManagementSystem.Repository.RentalBillRepository;
import com.example.EmployeeManagementSystem.Repository.RepairBillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BillService {
    private final RepairBillRepository repairBillRepository;
    private final RentalBillRepository rentalBillRepository;
    private final InvoiceRepository invoiceRepository;

    public BillService(RepairBillRepository repairBillRepository, RentalBillRepository rentalBillRepository, InvoiceRepository invoiceRepository) {
        this.repairBillRepository = repairBillRepository;
        this.rentalBillRepository = rentalBillRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<RepairBillDTO> getRepairBills(){
       List<RepairBill> repairBills=repairBillRepository.findByPaymentStatusNotInAndRepairCostGreaterThan(
               List.of(PaymentStatus.PAID,PaymentStatus.ACKNOWLEDGED),
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
        List<RentalBill> rentalBills=rentalBillRepository.findByPaymentStatusNotInAndAmountGreaterThan(
                List.of(PaymentStatus.PAID,PaymentStatus.ACKNOWLEDGED),
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

    public InvoiceDTO generateRepairInvoice(Long id) {
        RepairBill repairBill=repairBillRepository.findById(id).orElseThrow(
                ()->new RepairBillNotFoundException("Bill with Id: "+id+" Not Found")
        );

        if (invoiceRepository.existsByRepairBill(repairBill)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice already generated for this rental bill"
            );
        }
        repairBill.setPaymentStatus(PaymentStatus.PAID);
        Invoice invoice=new Invoice();
        invoice.setInvoiceType(InvoiceType.REPAIR);
        invoice.setRepairBill(repairBill);
        invoice.setAmount(repairBill.getRepairCost());
        invoice.setAmount(repairBill.getRepairCost());
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedDate(LocalDate.now());
        invoice.setVendor(repairBill.getDevice().getTechVendor());
        Invoice saved=invoiceRepository.save(invoice);
        return toInvoiceDTO(saved);
    }

    public InvoiceDTO generateRentalInvoice(Long id) {
        RentalBill rentalBill=rentalBillRepository.findById(id).orElseThrow(
                ()->new RentalBillNotFoundException("Bill with Id: "+id+" Not Found")
        );
        if (invoiceRepository.existsByRentalBill(rentalBill)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice already generated for this rental bill"
            );
        }
        rentalBill.setPaymentStatus(PaymentStatus.PAID);
        Invoice invoice=new Invoice();
        invoice.setInvoiceType(InvoiceType.RENTAL);
        invoice.setRentalBill(rentalBill);
        invoice.setAmount(rentalBill.getAmount());
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedDate(LocalDate.now());
        invoice.setVendor(rentalBill.getVendor());
        Invoice saved=invoiceRepository.save(invoice);
        return toInvoiceDTO(saved);
    }

    public InvoiceDTO toInvoiceDTO(Invoice invoice) {
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
}
