package com.example.EmployeeManagementSystem.service;


import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeaveAccrualServiceTest {

    @Mock
    EmployeeRepo employeeRepo;
    @Mock
    LeaveTypeRepository leaveTypeRepo;
    @Mock
    LeaveEntitlementRepository entitlementRepo;

    @InjectMocks
    LeaveAccrualService service;

    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void maleEmployee_doesNotAccrueMaternity() {
        Employee emp = employee(Gender.M);
        LeaveType maternity = leaveType("MATERNITY", true, Gender.F);

        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(leaveTypeRepo.findAll()).thenReturn(List.of(maternity));

        service.runMonthlyAccrual(LocalDate.of(2025, 6, 1));

        verify(entitlementRepo, never()).save(any());
    }

    @Test
    void femaleEmployee_accruesMaternity() {
        Employee emp = employee(Gender.F);
        LeaveType maternity = leaveType("MATERNITY", true, Gender.F);

        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(leaveTypeRepo.findAll()).thenReturn(List.of(maternity));
        when(entitlementRepo.findByEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.empty());

        // Simulate new entitlement creation
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyAccrual(LocalDate.of(2025, 6, 1));

        ArgumentCaptor<LeaveEntitlement> captor = ArgumentCaptor.forClass(LeaveEntitlement.class);
        verify(entitlementRepo, atLeastOnce()).save(captor.capture());

        LeaveEntitlement saved = captor.getAllValues().stream()
                .filter(e -> e.getAccruedThisYear().compareTo(BigDecimal.ONE) == 0)
                .findFirst().orElseThrow();

        assertThat(saved.getAccruedThisYear()).isEqualByComparingTo("1");
    }

    @Test
    void employeeJoiningAfterAccrualDate_isSkipped() {
        Employee emp = employee(Gender.M);
        emp.setJoined_at(LocalDate.of(2025, 6, 15)); // joins mid-month
        LeaveType casual = leaveType("CASUAL", false, null);

        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(leaveTypeRepo.findAll()).thenReturn(List.of(casual));

        service.runMonthlyAccrual(LocalDate.of(2025, 6, 1));

        verify(entitlementRepo, never()).save(any());
    }

    // --- helpers ---
    private Employee employee(Gender gender) {
        Employee e = new Employee();
        e.setEmployeeId(1L);
        e.setGender(gender);
        e.setStatus(Status.ACTIVE);
        e.setJoined_at(LocalDate.of(2023, 1, 1));
        return e;
    }

    private LeaveType leaveType(String name, boolean genderRestricted, Gender restriction) {
        LeaveType lt = new LeaveType();
        lt.setId(1);
        lt.setName(name);
        lt.setGenderRestricted(genderRestricted);
        lt.setGenderRestriction(restriction);
        lt.setCarriesForward(!name.equals("SICK"));
        lt.setMonthlyReset(name.equals("SICK"));
        return lt;
    }
}