package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.DTO.LeaveRequestDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Enum.LeaveType;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveEntitlementRepository;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeManagementSystem.Service.LeaveRequestService;
import com.example.EmployeeManagementSystem.Service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.EmployeeManagementSystem.Util.AuthUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
    @Mock
    NotificationService notificationService;

    @InjectMocks
    LeaveRequestService service;

    private MockedStatic<AuthUtil> authUtilMock;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authUtilMock = org.mockito.Mockito.mockStatic(AuthUtil.class);
    }

    @AfterEach
    void tearDown() {
        authUtilMock.close();
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
        doNothing().when(notificationService).notify(any(), any(), any());

        service.approveLeave(10L);

        assertThat(entitlement.getUsedThisYear()).isEqualByComparingTo("4");
    }

    @Test
    void createRequest_allowsCasualLeaveEqualToAvailableBalance() {
        Authentication authentication = mock(Authentication.class);
        authUtilMock.when(() -> AuthUtil.extractEmail(authentication)).thenReturn("emp@test.com");

        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmail("emp@test.com");
        employee.setStatus(Status.ACTIVE);
        employee.setRole(Role.EMPLOYEE);
        employee.setTimezone("UTC");
        employee.setName("Emp");

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(2));
        dto.setReason("Personal work");

        com.example.EmployeeManagementSystem.Entity.LeaveType casual =
                new com.example.EmployeeManagementSystem.Entity.LeaveType();
        casual.setId(2);
        casual.setName("CASUAL");
        casual.setCarriesForward(true);

        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(casual);
        entitlement.setYear(dto.getStartDate().getYear());
        entitlement.setOpeningBalance(BigDecimal.ONE);
        entitlement.setAccruedThisYear(BigDecimal.valueOf(3));
        entitlement.setUsedThisYear(BigDecimal.valueOf(2));

        when(employeeRepo.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findByName("CASUAL")).thenReturn(Optional.of(casual));
        when(leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                1L, 2, dto.getStartDate().getYear())).thenReturn(Optional.of(entitlement));
        when(leaveRequestRepo.checkDuplicate(1L, dto.getStartDate(), dto.getEndDate(), LeaveStatus.PENDING.name()))
                .thenReturn(0L);
        when(leaveRequestRepo.countOverlappingLeave(1L, dto.getStartDate(), dto.getEndDate()))
                .thenReturn(0L);
        when(leaveRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepo.findByRole(Role.ADMIN)).thenReturn(List.of());

        ResponseEntity<?> response = service.createRequest(authentication, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createRequest_rejectsCasualLeaveAboveAvailableBalance() {
        Authentication authentication = mock(Authentication.class);
        authUtilMock.when(() -> AuthUtil.extractEmail(authentication)).thenReturn("emp@test.com");

        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmail("emp@test.com");
        employee.setStatus(Status.ACTIVE);
        employee.setRole(Role.EMPLOYEE);
        employee.setTimezone("UTC");

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(3));

        com.example.EmployeeManagementSystem.Entity.LeaveType casual =
                new com.example.EmployeeManagementSystem.Entity.LeaveType();
        casual.setId(2);
        casual.setName("CASUAL");
        casual.setCarriesForward(true);

        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(casual);
        entitlement.setYear(dto.getStartDate().getYear());
        entitlement.setOpeningBalance(BigDecimal.ONE);
        entitlement.setAccruedThisYear(BigDecimal.ONE);
        entitlement.setUsedThisYear(BigDecimal.ONE);

        when(employeeRepo.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findByName("CASUAL")).thenReturn(Optional.of(casual));
        when(leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
                1L, 2, dto.getStartDate().getYear())).thenReturn(Optional.of(entitlement));

        ResponseEntity<?> response = service.createRequest(authentication, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response.getBody()).get("error").toString())
                .contains("exceeds available balance");
    }

    @Test
    void createRequest_allowsSickLeaveWithinSevenDaysAfterLeaveDate() {
        Authentication authentication = mock(Authentication.class);
        authUtilMock.when(() -> AuthUtil.extractEmail(authentication)).thenReturn("emp@test.com");

        Employee employee = activeEmployee();
        LocalDate sickDate = LocalDate.now().minusDays(1);

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setLeaveType(LeaveType.SICK);
        dto.setStartDate(sickDate);
        dto.setEndDate(sickDate);
        dto.setReason("Fever");

        com.example.EmployeeManagementSystem.Entity.LeaveType sick =
                new com.example.EmployeeManagementSystem.Entity.LeaveType();
        sick.setId(1);
        sick.setName("SICK");

        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setLeaveType(sick);
        entitlement.setYear(sickDate.getYear());
        entitlement.setOpeningBalance(BigDecimal.ZERO);
        entitlement.setAccruedThisYear(BigDecimal.ONE);
        entitlement.setUsedThisYear(BigDecimal.ZERO);

        when(employeeRepo.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepo.countApprovedSickDaysInMonth(employee, sickDate.getYear(), sickDate.getMonthValue()))
                .thenReturn(0L);
        when(leaveTypeRepository.findByName("SICK")).thenReturn(Optional.of(sick));
        when(leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(1L, 1, sickDate.getYear()))
                .thenReturn(Optional.of(entitlement));
        when(leaveRequestRepo.checkDuplicate(1L, sickDate, sickDate, LeaveStatus.PENDING.name()))
                .thenReturn(0L);
        when(leaveRequestRepo.countOverlappingLeave(1L, sickDate, sickDate)).thenReturn(0L);
        when(leaveRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepo.findByRole(Role.ADMIN)).thenReturn(List.of());

        ResponseEntity<?> response = service.createRequest(authentication, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO.class);
        assertThat(((com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO) response.getBody()).getLeaveType())
                .isEqualTo(LeaveType.SICK);
    }

    @Test
    void createRequest_convertsLateSickLeaveToUnpaidAfterSevenDays() {
        Authentication authentication = mock(Authentication.class);
        authUtilMock.when(() -> AuthUtil.extractEmail(authentication)).thenReturn("emp@test.com");

        Employee employee = activeEmployee();
        LocalDate sickDate = LocalDate.now().minusDays(8);

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setLeaveType(LeaveType.SICK);
        dto.setStartDate(sickDate);
        dto.setEndDate(sickDate);
        dto.setReason("Forgot to regularize sick leave");

        when(employeeRepo.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepo.checkDuplicate(1L, sickDate, sickDate, LeaveStatus.PENDING.name()))
                .thenReturn(0L);
        when(leaveRequestRepo.countOverlappingLeave(1L, sickDate, sickDate)).thenReturn(0L);
        when(leaveRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepo.findByRole(Role.ADMIN)).thenReturn(List.of());

        ResponseEntity<?> response = service.createRequest(authentication, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("warning").toString()).contains("within 7 days");
        com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO leave =
                (com.example.EmployeeManagementSystem.DTO.LeaveResponseDTO) body.get("leave");
        assertThat(leave.getLeaveType()).isEqualTo(LeaveType.UNPAID);
    }

    @Test
    void approveLeave_rejectsWhenPendingRequestWouldOverdrawBalance() {
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
        entitlement.setOpeningBalance(BigDecimal.ONE);
        entitlement.setAccruedThisYear(BigDecimal.ONE);
        entitlement.setUsedThisYear(BigDecimal.ONE); // only 1 day left

        when(leaveRequestRepo.findById(10L)).thenReturn(Optional.of(request));
        when(leaveTypeRepository.findByName("CASUAL")).thenReturn(Optional.of(casual));
        when(leaveEntitlementRepository.findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(1L, 2, 2026))
                .thenReturn(Optional.of(entitlement));

        assertThatThrownBy(() -> service.approveLeave(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient casual leave balance");
    }

    private Employee activeEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeId(1L);
        employee.setEmail("emp@test.com");
        employee.setStatus(Status.ACTIVE);
        employee.setRole(Role.EMPLOYEE);
        employee.setTimezone("UTC");
        employee.setName("Emp");
        return employee;
    }
}
