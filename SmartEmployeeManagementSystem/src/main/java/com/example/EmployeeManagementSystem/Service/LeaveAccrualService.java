package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Enum.Role;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
public class LeaveAccrualService {

    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualService.class);
    private static final BigDecimal ONE_DAY = BigDecimal.ONE;
    private static final Set<String> SUPPORTED_LEAVE_TYPES = Set.of("SICK", "CASUAL");
    private static final Set<Role> LEAVE_ACCRUAL_ROLES = Set.of(Role.EMPLOYEE, Role.MANAGER, Role.ADMIN);

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
        List<Employee> activeEmployees = employeeRepository.findByStatus(Status.ACTIVE).stream()
                .filter(employee -> employee.getRole() != null)
                .filter(employee -> LEAVE_ACCRUAL_ROLES.contains(employee.getRole()))
                .toList();
        List<LeaveType> allLeaveTypes = leaveTypeRepository.findAll();

        log.info("Starting monthly accrual for {} — {} active employees",
                accrualDate, activeEmployees.size());

        for (Employee employee : activeEmployees) {
            accrueForEmployee(employee, allLeaveTypes, accrualDate);
        }

        log.info("Monthly accrual complete for {}", accrualDate);
    }

    @Transactional
    public void grantInitialMonthlyAccrual(Employee employee, LocalDate joinDate) {
        if (joinDate == null) {
            joinDate = LocalDate.now();
        }

        LocalDate firstDayOfLastWeek = YearMonth.from(joinDate).atEndOfMonth().minusDays(6);
        if (!joinDate.isBefore(firstDayOfLastWeek)) {
            log.info("Initial leave accrual skipped for employee {} joining in the last week of month {}",
                    employee.getEmployeeId(), joinDate);
            return;
        }
        accrueForEmployee(employee, leaveTypeRepository.findAll(), joinDate);
        log.info("Initial monthly leave accrual complete for employee {} on {}",
                employee.getEmployeeId(), joinDate);
    }

    private void accrueForEmployee(Employee employee, List<LeaveType> leaveTypes, LocalDate accrualDate) {
        int year = accrualDate.getYear();

        for (LeaveType leaveType : leaveTypes) {
            if (!SUPPORTED_LEAVE_TYPES.contains(leaveType.getName())) {
                continue;
            }
            if (!isEligible(employee, leaveType, accrualDate)) {
                continue;
            }

            LeaveEntitlement entitlement = entitlementRepository
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year)
                    .orElseGet(() -> createNewEntitlement(employee, leaveType, year));

            entitlement.setAccruedThisYear(
                    entitlement.getAccruedThisYear().add(ONE_DAY)
            );

            entitlement.setClosingBalance(
                    entitlement.getOpeningBalance()
                            .add(entitlement.getAccruedThisYear())
                            .subtract(entitlement.getUsedThisYear())
            );

            entitlementRepository.save(entitlement);
            log.debug("Accrued 1 {} day for employee {} (year {})",
                    leaveType.getName(), employee.getEmployeeId(), year);
        }
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
                    .findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                            employee.getEmployeeId(), leaveType.getId(), year - 1)
                    .map(LeaveEntitlement::getClosingBalance)
                    .orElse(BigDecimal.ZERO);

            entitlement.setOpeningBalance(lastYearClosing);
        }

        return entitlementRepository.save(entitlement);
    }
}
