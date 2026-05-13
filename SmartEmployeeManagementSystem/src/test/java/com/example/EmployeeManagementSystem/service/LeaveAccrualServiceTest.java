package com.example.EmployeeManagementSystem.service;


import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Enum.Role;
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
    void femaleEmployee_doesNotAccrueMaternity() {
        Employee emp = employee(Gender.F);
        LeaveType maternity = leaveType("MATERNITY", true, Gender.F);

        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(emp));
        when(leaveTypeRepo.findAll()).thenReturn(List.of(maternity));

        service.runMonthlyAccrual(LocalDate.of(2025, 6, 1));

        verify(entitlementRepo, never()).save(any());
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

    @Test
    void newEmployeeJoiningBeforeMonthEnd_getsInitialSickAndCasualLeave() {
        Employee emp = employee(Gender.M);
        emp.setJoined_at(LocalDate.of(2025, 5, 8));
        LeaveType sick = leaveType("SICK", false, null);
        sick.setId(1);
        LeaveType casual = leaveType("CASUAL", false, null);
        casual.setId(2);

        when(leaveTypeRepo.findAll()).thenReturn(List.of(sick, casual));
        when(entitlementRepo.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.grantInitialMonthlyAccrual(emp, LocalDate.of(2025, 5, 8));

        ArgumentCaptor<LeaveEntitlement> captor = ArgumentCaptor.forClass(LeaveEntitlement.class);
        verify(entitlementRepo, atLeast(2)).save(captor.capture());

        List<LeaveEntitlement> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.getLeaveType().getName()).isEqualTo("SICK");
            assertThat(e.getAccruedThisYear()).isEqualByComparingTo(BigDecimal.ONE);
        });
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.getLeaveType().getName()).isEqualTo("CASUAL");
            assertThat(e.getAccruedThisYear()).isEqualByComparingTo(BigDecimal.ONE);
        });
    }

    @Test
    void newEmployeeJoiningInLastWeek_waitsForNextMonthAccrual() {
        Employee emp = employee(Gender.M);
        emp.setJoined_at(LocalDate.of(2025, 5, 25));

        service.grantInitialMonthlyAccrual(emp, LocalDate.of(2025, 5, 25));

        verifyNoInteractions(leaveTypeRepo);
        verify(entitlementRepo, never()).save(any());
    }

    @Test
    void newEmployeeJoiningBeforeLastWeek_getsInitialLeave() {
        Employee emp = employee(Gender.M);
        emp.setJoined_at(LocalDate.of(2025, 5, 24));
        LeaveType sick = leaveType("SICK", false, null);

        when(leaveTypeRepo.findAll()).thenReturn(List.of(sick));
        when(entitlementRepo.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.grantInitialMonthlyAccrual(emp, LocalDate.of(2025, 5, 24));

        verify(entitlementRepo, atLeastOnce()).save(any());
    }

    @Test
    void activeAdminReceivesMonthlySickAndCasualAccrual() {
        Employee admin = employee(Gender.M);
        admin.setRole(Role.ADMIN);
        LeaveType sick = leaveType("SICK", false, null);
        sick.setId(1);
        LeaveType casual = leaveType("CASUAL", false, null);
        casual.setId(2);

        when(employeeRepo.findByStatus(Status.ACTIVE)).thenReturn(List.of(admin));
        when(leaveTypeRepo.findAll()).thenReturn(List.of(sick, casual));
        when(entitlementRepo.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(entitlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runMonthlyAccrual(LocalDate.of(2025, 6, 1));

        ArgumentCaptor<LeaveEntitlement> captor = ArgumentCaptor.forClass(LeaveEntitlement.class);
        verify(entitlementRepo, atLeast(2)).save(captor.capture());

        assertThat(captor.getAllValues()).anySatisfy(e -> {
            assertThat(e.getLeaveType().getName()).isEqualTo("SICK");
            assertThat(e.getAccruedThisYear()).isEqualByComparingTo(BigDecimal.ONE);
        });
        assertThat(captor.getAllValues()).anySatisfy(e -> {
            assertThat(e.getLeaveType().getName()).isEqualTo("CASUAL");
            assertThat(e.getAccruedThisYear()).isEqualByComparingTo(BigDecimal.ONE);
        });
    }

    // --- helpers ---
    private Employee employee(Gender gender) {
        Employee e = new Employee();
        e.setEmployeeId(1L);
        e.setGender(gender);
        e.setStatus(Status.ACTIVE);
        e.setRole(Role.EMPLOYEE);
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
