package com.example.EmployeeManagementSystem.Scheduler;

import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.RentalBill;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.DeviceStatus;
import com.example.EmployeeManagementSystem.Enum.PaymentStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.DeviceRepository;
import com.example.EmployeeManagementSystem.Repository.RentalBillRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class MonthlyBillScheduler {
    private final VendorRepo vendorRepo;
    private final DeviceRepository deviceRepository;
    private final RentalBillRepository rentalBillRepository;


    public MonthlyBillScheduler(VendorRepo vendorRepo, DeviceRepository deviceRepository, RentalBillRepository rentalBillRepository) {
        this.vendorRepo = vendorRepo;
        this.deviceRepository = deviceRepository;
        this.rentalBillRepository = rentalBillRepository;
    }


    @Scheduled(cron = "0 0 1 1 * *")
    public void monthlyBillCalculator(){
        List<Vendor> vendors=vendorRepo.findByRole(Role.TECH_VENDOR);

        for(Vendor vendor:vendors){
            List<Device> devices=deviceRepository.findByStatus(vendor.getId(),
                    List.of(DeviceStatus.ASSIGNED.name(),DeviceStatus.UNDER_REPAIR.name()));
            BigDecimal amount=BigDecimal.ZERO;
            RentalBill rentalBill=new RentalBill();
            rentalBill.setBillDate(LocalDate.now());
            rentalBill.setDueDate(LocalDate.now().plusDays(7));
            rentalBill.setVendor(vendor);
            for(Device device:devices){
                amount=amount.add(device.getRentPerMonth());
            }
            rentalBill.setAmount(amount);
            rentalBill.setPaymentStatus(PaymentStatus.PENDING);
            rentalBillRepository.save(rentalBill);
        }
    }
}
