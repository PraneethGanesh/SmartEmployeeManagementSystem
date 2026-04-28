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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SickLeaveResetServiceTest {

    @Mock
    LeaveTypeRepository leaveTypeRepo;
    @Mock
    LeaveEntitlementRepository entitlementRepo;
    @InjectMocks SickLeaveResetService service;

    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void unusedSickDays_areZeroedAtMonthEnd() {
        LeaveType sick = sickLeaveType();
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.valueOf(3), BigDecimal.ONE);
        // accrued=3, used=1 → available=2 (should be wiped)

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(LocalDate.of(2025, 6, 30));

        // After reset: accrued should equal used → available = 0
        assertThat(entitlement.getAccruedThisYear())
                .isEqualByComparingTo(entitlement.getUsedThisYear());
        assertThat(entitlement.getAvailableBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void fullyUsedSickLeave_noChangeNeeded() {
        LeaveType sick = sickLeaveType();
        LeaveEntitlement entitlement = entitlement(sick, BigDecimal.ONE, BigDecimal.ONE);
        // accrued=1, used=1 → available=0, nothing to wipe

        when(leaveTypeRepo.findByMonthlyResetTrue()).thenReturn(List.of(sick));
        when(entitlementRepo.findByLeaveTypeIdAndYear(sick.getId(), 2025))
                .thenReturn(List.of(entitlement));
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyReset(LocalDate.of(2025, 6, 30));

        // accrued unchanged
        assertThat(entitlement.getAccruedThisYear()).isEqualByComparingTo("1");
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

    private LeaveEntitlement entitlement(LeaveType type,
                                         BigDecimal accrued,
                                         BigDecimal used) {
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
