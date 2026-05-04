package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import com.example.EmployeeManagementSystem.Service.SickLeaveResetService;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SickLeaveResetServiceTest {

    @Mock
    LeaveTypeRepository leaveTypeRepo;
    @Mock
    LeaveEntitlementRepository entitlementRepo;
    @InjectMocks
    SickLeaveResetService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void unusedSickDays_areZeroedAtMonthEnd() {
        LeaveType sick = sickLeaveType();
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.valueOf(3), BigDecimal.ONE);
        // accrued=3, used=1, available=2 → should be wiped

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(LocalDate.of(2025, 6, 30));

        // After reset: accrued == used, available == 0
        assertThat(entitlement.getAccruedThisYear())
                .isEqualByComparingTo(entitlement.getUsedThisYear());
        assertThat(entitlement.getAvailableBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void fullyUsedSickLeave_noChangeNeeded() {
        LeaveType sick = sickLeaveType();
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.ONE, BigDecimal.ONE);
        // accrued=1, used=1, available=0 → nothing to wipe

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(LocalDate.of(2025, 6, 30));

        // accrued unchanged — still equals used
        assertThat(entitlement.getAccruedThisYear()).isEqualByComparingTo("1");
    }

    // FIX: New test — ensures reset does NOT alter a negative balance (over-used edge case)
    @Test
    void negativeBalance_isNotTouched() {
        LeaveType sick = sickLeaveType();
        // accrued=0, used=1, available=-1 (negative — already over-consumed)
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.ZERO, BigDecimal.ONE);

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(LocalDate.of(2025, 6, 30));

        // available is <= 0 so no reset should happen; accrued stays 0
        assertThat(entitlement.getAccruedThisYear()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // FIX: New test — verifies correct year/month is used when resetDate is end-of-month
    //      (guards against the midnight-fire fragility in the scheduler)
    @Test
    void resetDate_usesCorrectYearFromPassedDate_notFromSystemClock() {
        LeaveType sick = sickLeaveType();
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.valueOf(2), BigDecimal.ZERO);

        LocalDate endOfJune2025 = YearMonth.of(2025, 6).atEndOfMonth(); // 2025-06-30

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(endOfJune2025);

        // Should query year=2025, not system year — verified by mock setup using 2025 only
        verify(entitlementRepo).findByLeaveTypeIdAndYear(sick.getId(), 2025);
        assertThat(entitlement.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- helpers ---

    private LeaveType sickLeaveType() {
        LeaveType lt = new LeaveType();
        lt.setId(1);
        lt.setName("SICK");
        lt.setMonthlyReset(true);
        lt.setCarriesForward(false);
        return lt;
    }

    private LeaveEntitlement entitlement(LeaveType type, BigDecimal accrued, BigDecimal used) {
        Employee emp = new Employee();
        emp.setEmployeeId(1L);
        LeaveEntitlement e = new LeaveEntitlement();
        e.setEmployee(emp);
        e.setLeaveType(type);
        e.setYear(2025);
        e.setOpeningBalance(BigDecimal.ZERO);
        e.setAccruedThisYear(accrued);
        e.setUsedThisYear(used);
        e.setClosingBalance(BigDecimal.ZERO);
        return e;
    }
}