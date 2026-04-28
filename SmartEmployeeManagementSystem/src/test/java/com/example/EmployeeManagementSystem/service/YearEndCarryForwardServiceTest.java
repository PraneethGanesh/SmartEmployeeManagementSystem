package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveWarningRepository;
import com.example.EmployeeManagementSystem.Repository.YearEndCarryForwardLogRepository;
import com.example.EmployeeManagementSystem.Service.YearEndCarryForwardService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

import static org.assertj.core.api.Assertions.assertThat;
class YearEndCarryForwardServiceTest {

    @Mock
    EmployeeRepo employeeRepo;
    @Mock
    LeaveEntitlementRepository entitlementRepo;
    @Mock
    YearEndCarryForwardLogRepository logRepo;
    @Mock
    LeaveWarningRepository warningRepo;
    @InjectMocks
    YearEndCarryForwardService service;

    @BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void whenTotalUnderCap_noCapApplied() {
        Employee emp = employee();
        LeaveType casual = carryForwardType("CASUAL");

        // casual: opening=0, accrued=8, used=2 → closing=6
        LeaveEntitlement ent = entitlement(emp, casual, 0, 8, 2);

        when(logRepo.existsByProcessedYear(2025)).thenReturn(false);
        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(entitlementRepo.findByEmployeeEmployeeIdAndYear(emp.getEmployeeId(), 2025))
                .thenReturn(List.of(ent));
        when(entitlementRepo.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(any(), any(), eq(2026)))
                .thenReturn(Optional.empty());
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runYearEnd(2025);

        // closing balance = 6, no cap needed, nothing wasted
        assertThat(ent.getClosingBalance()).isEqualByComparingTo("6");
        verify(warningRepo, never()).save(any());
    }

    @Test
    void whenTotalExceedsCap_capAppliedProportionally() {
        Employee emp = employee();
        LeaveType casual    = carryForwardType("CASUAL");
        LeaveType maternity = carryForwardType("MATERNITY");

        // casual: closing=20, maternity: closing=16 → total=36, over cap by 6
        LeaveEntitlement casualEnt    = entitlement(emp, casual,    0, 20, 0);
        LeaveEntitlement maternityEnt = entitlement(emp, maternity, 0, 16, 0);

        when(logRepo.existsByProcessedYear(2025)).thenReturn(false);
        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(entitlementRepo.findByEmployeeEmployeeIdAndYear(emp.getEmployeeId(), 2025))
                .thenReturn(List.of(casualEnt, maternityEnt));
        when(entitlementRepo.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(any(), any(), eq(2026)))
                .thenReturn(Optional.empty());
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runYearEnd(2025);

        // casual: 20/36 * 30 = 16.67, maternity: 16/36 * 30 = 13.33, total = 30
        BigDecimal casualCapped    = casualEnt.getClosingBalance();
        BigDecimal maternityCapped = maternityEnt.getClosingBalance();
        BigDecimal total           = casualCapped.add(maternityCapped);

        assertThat(total).isEqualByComparingTo("30.00");
        verify(warningRepo, atLeastOnce()).save(any()); // wasted warning issued
    }

    @Test
    void idempotencyGuard_preventsDoubleRun() {
        when(logRepo.existsByProcessedYear(2025)).thenReturn(true);

        service.runYearEnd(2025);

        verify(employeeRepo, never()).findByStatus(any());
    }

    // --- helpers ---
    private Employee employee() {
        Employee e = new Employee(); e.setEmployeeId(1L); return e;
    }

    private LeaveType carryForwardType(String name) {
        LeaveType lt = new LeaveType();
        lt.setId(name.equals("CASUAL") ? 2 : 3);
        lt.setName(name);
        lt.setCarriesForward(true);
        lt.setMonthlyReset(false);
        return lt;
    }

    private LeaveEntitlement entitlement(Employee emp, LeaveType type,
                                         int opening, int accrued, int used) {
        LeaveEntitlement e = new LeaveEntitlement();
        e.setEmployee(emp); e.setLeaveType(type); e.setYear(2025);
        e.setOpeningBalance(BigDecimal.valueOf(opening));
        e.setAccruedThisYear(BigDecimal.valueOf(accrued));
        e.setUsedThisYear(BigDecimal.valueOf(used));
        e.setClosingBalance(BigDecimal.ZERO);
        return e;
    }
}
