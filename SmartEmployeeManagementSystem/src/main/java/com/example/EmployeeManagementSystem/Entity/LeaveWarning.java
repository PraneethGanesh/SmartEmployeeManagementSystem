package com.example.EmployeeManagementSystem.Entity;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_warning")
public class LeaveWarning {

    public enum WarningType {
        APPROACHING_28,   // 2 months before cap
        APPROACHING_29,   // 1 month before cap
        CAP_REACHED,      // exactly at 30
        DAY_WASTED        // already at 30, day lost
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WarningType warningType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal carryForwardBalance;

    @Column(nullable = false)
    private LocalDate warningDate;

    @Column(nullable = false)
    private boolean isRead = false;

    // --- getters & setters ---
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public WarningType getWarningType() { return warningType; }
    public String getMessage() { return message; }
    public BigDecimal getCarryForwardBalance() { return carryForwardBalance; }
    public LocalDate getWarningDate() { return warningDate; }
    public boolean isRead() { return isRead; }

    public void setEmployee(Employee e) { this.employee = e; }
    public void setWarningType(WarningType wt) { this.warningType = wt; }
    public void setMessage(String m) { this.message = m; }
    public void setCarryForwardBalance(BigDecimal b) { this.carryForwardBalance = b; }
    public void setWarningDate(LocalDate d) { this.warningDate = d; }
    public void setRead(boolean r) { this.isRead = r; }
}