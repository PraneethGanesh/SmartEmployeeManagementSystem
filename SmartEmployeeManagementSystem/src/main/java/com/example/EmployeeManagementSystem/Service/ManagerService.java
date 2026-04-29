package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.DeviceAssignment;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.AssignmentStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.Status;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Repository.DeviceAssignmentRepo;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.LeaveRequestRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManagerService {
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final LeaveRequestRepo leaveRequestRepo;
    private final DeviceAssignmentRepo deviceAssignmentRepo;

    public ManagerService(EmployeeRepo employeeRepo,
                          PasswordEncoder passwordEncoder,
                          LeaveRequestRepo leaveRequestRepo,
                          DeviceAssignmentRepo deviceAssignmentRepo) {
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
        this.leaveRequestRepo = leaveRequestRepo;
        this.deviceAssignmentRepo = deviceAssignmentRepo;
    }

    public Employee createEmp(EmployeeDetails employeeDetails, Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Employee manager= employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manager :"+email+"Not found")
        );
        Employee employee=new Employee();
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        employee.setGender(employeeDetails.getGender());
        employee.setDept(manager.getDept());
        employee.setRole(Role.EMPLOYEE);
        employee.setManager(manager);
        employee.setTimezone(employeeDetails.getTimezone());
        return employeeRepo.save(employee);
    }


    public List<Employee> getAllEmployeeByManager(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manger:"+email+" not found")
        );
        return employeeRepo.findByManager(manager);
    }

    public List<ManagerLeaveResponseDTO> getAllLeaveRequestByManager(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
       Employee manager=employeeRepo.findByEmail(email).orElseThrow(
               ()->new EmployeeNotFound("Manager with "+email+" not found")
       );
       List<LeaveRequest> requests=leaveRequestRepo.findByEmployee_Manager(manager);
       return requests.stream().map(request->toManagerLeaveResponseDTO(request)).toList();
    }

    public List<EmployeeDTO> getUsers(){
        List<Employee> employeeList=employeeRepo.findByRole(Role.USER);
        return employeeList.stream().map(this::toDTO).toList();
    }

    public ResponseEntity<?> promoteUser(Authentication authentication, PromoteRequest promoteRequest){
        Employee user=employeeRepo.findByEmail(promoteRequest.getEmail()).orElseThrow(
                ()->new EmployeeNotFound("Employee:"+promoteRequest.getEmail()+" not found")
        );
        if(user.getRole()!=Role.USER){
            return ResponseEntity.ok("Employee already promoted");
        }
        String managerEmail=AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(managerEmail).orElseThrow(
                ()->new EmployeeNotFound("Manager:"+managerEmail+" not found")
        );

        user.setRole(Role.EMPLOYEE);
        user.setManager(manager);
        user.setDept(manager.getDept());
        user.setPassword(passwordEncoder.encode(promoteRequest.getPassword()));
        Employee promotedEmployee=employeeRepo.save(user);
        return ResponseEntity.ok(toDTO(promotedEmployee));
    }
    
    public EmployeeDTO toDTO(Employee employee){
        EmployeeDTO employeeDTO=new EmployeeDTO();
        employeeDTO.setName(employee.getName());
        employeeDTO.setEmail(employee.getEmail());
        employeeDTO.setDept(employee.getDept());
        employeeDTO.setPassword(employee.getPassword());
        employeeDTO.setTimezone(employee.getTimezone());
        return employeeDTO;
    }

    public Employee createAdmin(EmployeeDetails employeeDetails) {
        Employee employee=new Employee();
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        employee.setDept("Main");
        employee.setRole(Role.ADMIN);
        employee.setTimezone(employeeDetails.getTimezone());
        return employeeRepo.save(employee);
    }

    public List<LeaveResponseDTO> getAllThePendingLeaveRequests(Authentication authentication) {
        String email=AuthUtil.extractEmail(authentication);
        Employee manager=employeeRepo.findByEmail(email).orElseThrow(
                ()->new EmployeeNotFound("Manager with "+email+" not found")
        );
        List<LeaveRequest> leaveRequests=leaveRequestRepo.findByStatusAndEmployee_Manager(LeaveStatus.PENDING,manager);
        return leaveRequests.stream()
                .map(this::convertToLeaveResponseDTO)
                .collect(Collectors.toList());
    }

    public ManagerEmployeeDetailsDTO getEmployeeDetailsByName( String name) {

        Employee employee = employeeRepo.findByName(name).orElseThrow(
                () -> new EmployeeNotFound("Employee:" + name + " not found")
        );



        ManagerEmployeeDetailsDTO dto = new ManagerEmployeeDetailsDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setDept(employee.getDept());
        dto.setStatus(employee.getStatus());
        dto.setAttendance(toAttendance(employee.getStatus()));
        dto.setDevices(deviceAssignmentRepo
                .findByAssignedToEmployeeIdAndStatus(employee.getEmployeeId(), AssignmentStatus.ACTIVE)
                .stream()
                .map(DeviceAssignment::getDevice)
                .map(this::toDeviceResponseDTO)
                .toList());
        return dto;
    }

    private LeaveResponseDTO convertToLeaveResponseDTO(LeaveRequest leaveRequest) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setLeaveRequestId(leaveRequest.getId());
        dto.setEmployeeId(leaveRequest.getEmployee().getEmployeeId());
        dto.setLeaveType(leaveRequest.getLeaveType());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setReason(leaveRequest.getReason());
        dto.setStatus(leaveRequest.getStatus());
        return dto;
    }

    private ManagerLeaveResponseDTO toManagerLeaveResponseDTO(LeaveRequest leaveRequest){
        ManagerLeaveResponseDTO managerLeaveResponseDTO=new ManagerLeaveResponseDTO();
        managerLeaveResponseDTO.setEmployeeName(leaveRequest.getEmployee().getName());
        managerLeaveResponseDTO.setLeaveType(leaveRequest.getLeaveType());
        managerLeaveResponseDTO.setStartDate(leaveRequest.getStartDate());
        managerLeaveResponseDTO.setEndDate(leaveRequest.getEndDate());
        managerLeaveResponseDTO.setNumberOfDays(
                ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1);
        managerLeaveResponseDTO.setStatus(leaveRequest.getStatus());
        managerLeaveResponseDTO.setReason(leaveRequest.getReason());
        return managerLeaveResponseDTO;
    }

    private String toAttendance(Status status) {
        if (status == Status.ON_LEAVE) {
            return "ON_LEAVE";
        }
        if (status == Status.ACTIVE) {
            return "PRESENT";
        }
        return status.name();
    }

    private DeviceResponseDTO toDeviceResponseDTO(Device device) {
        DeviceResponseDTO dto = new DeviceResponseDTO();
        dto.setId(device.getId());
        dto.setDeviceName(device.getDeviceName());
        dto.setBrand(device.getBrand());
        dto.setDeviceType(device.getDeviceType());
        dto.setDeviceStatus(device.getDeviceStatus());
        dto.setWarrantyExpiryDate(device.getWarrantyExpiryDate());

        if (device.getTechVendor() != null) {
            dto.setVendorName(device.getTechVendor().getName());
        }

        DeviceAssignment assignment = device.getCurrentAssignment();
        if (assignment != null && assignment.getStatus() == AssignmentStatus.ACTIVE) {
            dto.setAssignedDate(assignment.getAssignedDate());
            if (assignment.getAssignedTo() != null) {
                dto.setAssignedEmployeeId(assignment.getAssignedTo().getEmployeeId());
                dto.setAssignedEmployeeName(assignment.getAssignedTo().getName());
            }
        }

        return dto;
    }
}
