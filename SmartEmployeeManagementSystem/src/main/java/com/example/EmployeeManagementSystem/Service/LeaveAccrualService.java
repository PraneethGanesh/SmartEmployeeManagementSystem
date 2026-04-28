package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveAccrualService {

    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualService.class);
    private static final BigDecimal ONE_DAY = BigDecimal.ONE;

    private final EmployeeRepo employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementRepository entitlementRepository;

    public LeaveAccrualService(EmployeeRepo employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveEntitlementRepository entitlementRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @Transactional
    public void runMonthlyAccrual(LocalDate accrualDate) {
        int year = accrualDate.getYear();

        List<Employee> activeEmployees = employeeRepository.findByStatus(Status.ACTIVE);
        List<LeaveType> allLeaveTypes = leaveTypeRepository.findAll();

        log.info("Starting monthly accrual for {} — {} active employees",
                accrualDate, activeEmployees.size());

        for (Employee employee : activeEmployees) {
            for (LeaveType leaveType : allLeaveTypes) {

                if (!isEligible(employee, leaveType, accrualDate)) {
                    continue;
                }

                // Find or create entitlement row for this employee/type/year
                LeaveEntitlement entitlement = entitlementRepository
                        .findByEmployeeIdAndLeaveTypeIdAndYear(
                                employee.getEmployeeId(), leaveType.getId(), year)
                        .orElseGet(() -> createNewEntitlement(employee, leaveType, year));

                // Add 1 day accrual
                entitlement.setAccruedThisYear(
                        entitlement.getAccruedThisYear().add(ONE_DAY)
                );

                entitlementRepository.save(entitlement);
                log.debug("Accrued 1 {} day for employee {} (year {})",
                        leaveType.getName(), employee.getEmployeeId(), year);
            }
        }

        log.info("Monthly accrual complete for {}", accrualDate);
    }

    // ---------------------------------------------------------------
    // Eligibility check — single place for all rules
    // ---------------------------------------------------------------
    private boolean isEligible(Employee employee, LeaveType leaveType, LocalDate accrualDate) {

        // 1. Gender restriction (Maternity = female only)
        if (leaveType.isGenderRestricted()) {
            Gender restriction = leaveType.getGenderRestriction();
            if (restriction != null && restriction != employee.getGender()) {
                return false;
            }
        }

        // 2. Employee must have joined ON OR BEFORE the accrual date
        //    (no accrual for employees joining after the 1st of this month)
        if (employee.getJoined_at() != null && employee.getJoined_at().isAfter(accrualDate)) {
            return false;
        }

        return true;
    }

    // ---------------------------------------------------------------
    // Creates a fresh entitlement row, carrying forward previous year's
    // closing balance if this is the first row of the year
    // ---------------------------------------------------------------
    private LeaveEntitlement createNewEntitlement(Employee employee,
                                                  LeaveType leaveType,
                                                  int year) {
        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(leaveType);
        entitlement.setYear(year);

        // Carry forward last year's closing balance (if the type supports it)
        if (leaveType.isCarriesForward()) {
            BigDecimal lastYearClosing = entitlementRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year - 1)
                    .map(LeaveEntitlement::getClosingBalance)
                    .orElse(BigDecimal.ZERO);

            entitlement.setOpeningBalance(lastYearClosing);
        }

        return entitlementRepository.save(entitlement);
    }
}
