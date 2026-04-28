package com.example.EmployeeManagementSystem.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_entitlement", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "year"}))
public class LeaveEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal accruedThisYear = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal usedThisYear = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    // Convenience: current available = opening + accrued - used
    @Transient
    public BigDecimal getAvailableBalance() {
        return openingBalance.add(accruedThisYear).subtract(usedThisYear);
    }

    // --- getters & setters ---
    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public Integer getYear() {
        return year;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getAccruedThisYear() {
        return accruedThisYear;
    }

    public BigDecimal getUsedThisYear() {
        return usedThisYear;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setEmployee(Employee e) {
        this.employee = e;
    }

    public void setLeaveType(LeaveType lt) {
        this.leaveType = lt;
    }

    public void setYear(Integer y) {
        this.year = y;
    }

    public void setOpeningBalance(BigDecimal b) {
        this.openingBalance = b;
    }

    public void setAccruedThisYear(BigDecimal a) {
        this.accruedThisYear = a;
    }

    public void setUsedThisYear(BigDecimal u) {
        this.usedThisYear = u;
    }

    public void setClosingBalance(BigDecimal c) {
        this.closingBalance = c;
    }
}