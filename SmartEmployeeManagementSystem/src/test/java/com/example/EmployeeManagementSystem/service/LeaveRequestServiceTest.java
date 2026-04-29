package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveType;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LeaveRequestServiceTest {

    @Mock
    LeaveRequestRepo leaveRequestRepo;
    @Mock
    EmployeeRepo employeeRepo;
    @Mock
    LeaveTypeRepository leaveTypeRepository;
    @Mock
    LeaveEntitlementRepository leaveEntitlementRepository;

    @InjectMocks
    LeaveRequestService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void approveLeave_updatesUsedEntitlementDays() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);

        LeaveRequest request = new LeaveRequest();
        request.setId(10L);
        request.setEmployee(employee);
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 6));
        request.setStatus(LeaveStatus.PENDING);

        com.example.EmployeeManagementSystem.Entity.LeaveType casual =
                new com.example.EmployeeManagementSystem.Entity.LeaveType();
        casual.setId(2);
        casual.setName("CASUAL");
        casual.setCarriesForward(true);

        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(casual);
        entitlement.setYear(2026);
        entitlement.setOpeningBalance(BigDecimal.ZERO);
        entitlement.setAccruedThisYear(BigDecimal.TEN);
        entitlement.setUsedThisYear(BigDecimal.ONE);
        entitlement.setClosingBalance(BigDecimal.ZERO);

        when(leaveRequestRepo.findById(10L)).thenReturn(Optional.of(request));
        when(leaveRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leaveTypeRepository.findByName("CASUAL")).thenReturn(Optional.of(casual));
        when(leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(1L, 2, 2026))
                .thenReturn(Optional.of(entitlement));
        when(leaveEntitlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.approveLeave(10L);

        assertThat(entitlement.getUsedThisYear()).isEqualByComparingTo("4");
    }
}
