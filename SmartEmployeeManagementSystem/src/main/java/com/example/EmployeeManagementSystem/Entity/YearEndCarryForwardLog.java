package com.example.EmployeeManagementSystem.Entity;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "year_end_carry_forward_log")
public class YearEndCarryForwardLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer processedYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal closingBalance;      // raw closing before cap

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal cappedBalance;       // after global 30-day cap applied

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal daysWasted = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal openingNextYear;     // written into next year's entitlement

    @Column(nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    // --- getters & setters ---
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public Integer getProcessedYear() { return processedYear; }
    public LeaveType getLeaveType() { return leaveType; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public BigDecimal getCappedBalance() { return cappedBalance; }
    public BigDecimal getDaysWasted() { return daysWasted; }
    public BigDecimal getOpeningNextYear() { return openingNextYear; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    public void setEmployee(Employee e) { this.employee = e; }
    public void setProcessedYear(Integer y) { this.processedYear = y; }
    public void setLeaveType(LeaveType lt) { this.leaveType = lt; }
    public void setClosingBalance(BigDecimal b) { this.closingBalance = b; }
    public void setCappedBalance(BigDecimal b) { this.cappedBalance = b; }
    public void setDaysWasted(BigDecimal d) { this.daysWasted = d; }
    public void setOpeningNextYear(BigDecimal b) { this.openingNextYear = b; }
    public void setProcessedAt(LocalDateTime dt) { this.processedAt = dt; }
}
