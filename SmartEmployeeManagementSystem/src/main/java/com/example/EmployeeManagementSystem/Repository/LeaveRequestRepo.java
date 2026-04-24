package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeManagementSystem.Enum.LeaveStatus;
import com.example.EmployeeManagementSystem.Enum.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepo extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStatus(LeaveStatus status);

    /**
     * Find leave requests by start date and status
     * Used by scheduler to find leaves starting today
     */
    List<LeaveRequest> findByStartDateAndStatus(LocalDate startDate, LeaveStatus status);

    // Fix the checkDuplicate query (fix parameter names)
    @Query(value = "SELECT COUNT(*) " +
            "FROM leave_request l " +
            "WHERE l.employee_id = :employeeId " +
            "AND l.start_date = :startDate " +
            "AND l.end_date = :endDate " +
            "AND l.status=:status",nativeQuery = true)
    long checkDuplicate(@Param("employeeId") long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") String status);

    // Fix overlapping leave query (was commented in your code)
    @Query(value = "select COUNT(*) " +
            "from leave_request l " +
            "where l.employee_id=:employeeId " +
            "AND l.status IN ('APPROVED','PENDING') " +
            "AND l.start_date <= :endDate " +
            "AND l.end_date >= :startDate",nativeQuery = true)
    long countOverlappingLeave(
            @Param("employeeId") long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Add missing methods
    List<LeaveRequest> findByEmployeeOrderByStartDateDesc(Employee employee);


    List<LeaveRequest> findByEndDateAndStatus(LocalDate endDate, LeaveStatus status);

    // Add for leave duration validation
    @Query("SELECT COALESCE(SUM(DATEDIFF(l.endDate, l.startDate) + 1), 0) " +
            "FROM LeaveRequest l WHERE l.employee = :employee " +
            "AND l.leaveType = :leaveType " +
            "AND YEAR(l.startDate) = :year " +
            "AND l.status IN ('APPROVED', 'PENDING')")
    long countDaysByEmployeeAndLeaveTypeAndYear(@Param("employee") Employee employee,
                                                @Param("leaveType") LeaveType leaveType,
                                                @Param("year") int year);

    @Query("SELECT lr FROM LeaveRequest lr JOIN FETCH lr.employee WHERE lr.status = :status")
    List<LeaveRequest> findApprovedLeavesWithEmployee(LeaveStatus status);


    List<LeaveRequest> findByEmployee_Manager(Employee manager);

}
