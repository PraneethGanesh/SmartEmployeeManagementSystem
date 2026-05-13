package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.DTO.EmployeeDetails;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Repository.DeviceAssignmentRepo;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Service.LeaveAccrualService;
import com.example.EmployeeManagementSystem.Service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerServiceTest {

    @Mock
    EmployeeRepo employeeRepo;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    LeaveRequestRepo leaveRequestRepo;
    @Mock
    DeviceAssignmentRepo deviceAssignmentRepo;
    @Mock
    LeaveAccrualService leaveAccrualService;

    ManagerService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ManagerService(
                employeeRepo,
                passwordEncoder,
                leaveRequestRepo,
                deviceAssignmentRepo,
                leaveAccrualService
        );
    }

    @Test
    void createAdminGrantsInitialSickAndCasualAccrual() {
        EmployeeDetails details = new EmployeeDetails();
        details.setName("Project Admin");
        details.setEmail("admin@example.com");
        details.setPassword("secret");
        details.setGender(Gender.F);
        details.setTimezone("Asia/Kolkata");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(employeeRepo.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setEmployeeId(42L);
            employee.setJoined_at(LocalDate.of(2026, 5, 13));
            return employee;
        });

        Employee saved = service.createAdmin(details);

        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getGender()).isEqualTo(Gender.F);
        verify(leaveAccrualService).grantInitialMonthlyAccrual(saved, LocalDate.of(2026, 5, 13));
    }
}
