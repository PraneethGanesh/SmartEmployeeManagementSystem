package com.example.EmployeeManagementSystem.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class LeaveDashboardResponse {

    private Long   employeeId;
    private String employeeName;
    private int    year;

    // Individual leave balances
    private LeaveBalanceDto sick;
    private LeaveBalanceDto casual;
    private LeaveBalanceDto maternity;       // null for non-female employees

    // Carry-forward meter (casual + maternity combined)
    private CarryForwardMeterDto carryForwardMeter;

    // Generic balances keyed by leave type name, e.g. SICK, CASUAL, MATERNITY.
    private Map<String, LeaveBalanceDto> leaveBalances;

    // Active warnings (unread)
    private List<WarningDto> warnings;

    // ── nested DTOs ──────────────────────────────────────────

    public static class LeaveBalanceDto {
        private String     leaveType;
        private BigDecimal opening;
        private BigDecimal accrued;
        private BigDecimal used;
        private BigDecimal available;
        private boolean    carriesForward;

        public LeaveBalanceDto(String leaveType, BigDecimal opening,
                               BigDecimal accrued, BigDecimal used,
                               BigDecimal available, boolean carriesForward) {
            this.leaveType     = leaveType;
            this.opening       = opening;
            this.accrued       = accrued;
            this.used          = used;
            this.available     = available;
            this.carriesForward = carriesForward;
        }

        public String     getLeaveType()      { return leaveType; }
        public BigDecimal getOpening()        { return opening; }
        public BigDecimal getAccrued()        { return accrued; }
        public BigDecimal getUsed()           { return used; }
        public BigDecimal getAvailable()      { return available; }
        public boolean    isCarriesForward()  { return carriesForward; }
    }

    public static class CarryForwardMeterDto {
        private BigDecimal current;     // e.g. 28.00
        private int        cap;         // always 30
        private String     label;       // "28/30"
        private String     status;      // SAFE | WARNING | CRITICAL | CAPPED

        public CarryForwardMeterDto(BigDecimal current, int cap) {
            this.current = current;
            this.cap     = cap;
            this.label   = current.toPlainString() + "/" + cap;
            this.status  = resolveStatus(current, cap);
        }

        private String resolveStatus(BigDecimal current, int cap) {
            int cmp = current.compareTo(BigDecimal.valueOf(cap));
            if (cmp >= 0)                                          return "CAPPED";
            if (current.compareTo(BigDecimal.valueOf(cap - 1)) >= 0) return "CRITICAL";
            if (current.compareTo(BigDecimal.valueOf(cap - 2)) >= 0) return "WARNING";
            return "SAFE";
        }

        public BigDecimal getCurrent() { return current; }
        public int        getCap()     { return cap; }
        public String     getLabel()   { return label; }
        public String     getStatus()  { return status; }
    }

    public static class WarningDto {
        private Long   id;
        private String type;
        private String message;
        private String date;

        public WarningDto(Long id, String type, String message, String date) {
            this.id = id; this.type = type;
            this.message = message; this.date = date;
        }

        public Long   getId()      { return id; }
        public String getType()    { return type; }
        public String getMessage() { return message; }
        public String getDate()    { return date; }
    }

    // ── root getters & setters ───────────────────────────────

    public Long                getEmployeeId()       { return employeeId; }
    public String              getEmployeeName()     { return employeeName; }
    public int                 getYear()             { return year; }
    public LeaveBalanceDto     getSick()             { return sick; }
    public LeaveBalanceDto     getCasual()           { return casual; }
    public LeaveBalanceDto     getMaternity()        { return maternity; }
    public CarryForwardMeterDto getCarryForwardMeter(){ return carryForwardMeter; }
    public Map<String, LeaveBalanceDto> getLeaveBalances() { return leaveBalances; }
    public List<WarningDto>    getWarnings()         { return warnings; }

    public void setEmployeeId(Long id)                        { this.employeeId = id; }
    public void setEmployeeName(String n)                     { this.employeeName = n; }
    public void setYear(int y)                                { this.year = y; }
    public void setSick(LeaveBalanceDto b)                    { this.sick = b; }
    public void setCasual(LeaveBalanceDto b)                  { this.casual = b; }
    public void setMaternity(LeaveBalanceDto b)               { this.maternity = b; }
    public void setCarryForwardMeter(CarryForwardMeterDto m)  { this.carryForwardMeter = m; }
    public void setLeaveBalances(Map<String, LeaveBalanceDto> b) { this.leaveBalances = b; }
    public void setWarnings(List<WarningDto> w)               { this.warnings = w; }
}
