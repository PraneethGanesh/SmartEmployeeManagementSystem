package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.LeaveEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LeaveEntitlementRepository extends JpaRepository<LeaveEntitlement, Long> {

    Optional<LeaveEntitlement> findByEmployeeEmployeeIdAndLeaveTypeIdAndYear(
            Long employeeId, Integer leaveTypeId, Integer year);

    List<LeaveEntitlement> findByEmployeeEmployeeIdAndYear(Long employeeId, Integer year);

    @Query("""
        SELECT COALESCE(SUM(e.closingBalance), 0)
        FROM LeaveEntitlement e
        WHERE e.employee.employeeId = :empId
          AND e.leaveType.carriesForward = true
          AND e.year IN :years
    """)
    BigDecimal sumCarryForwardBalances(
            @Param("empId") Long employeeId,
            @Param("years") List<Integer> years);

    List<LeaveEntitlement> findByLeaveTypeIdAndYear(Integer leaveTypeId, Integer year);
}