package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.Gender;
import jakarta.persistence.*;

@Entity
@Table(name = "leave_type")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;                    // SICK, CASUAL, MATERNITY

    @Column(nullable = false)
    private boolean carriesForward;

    @Column(nullable = false)
    private boolean monthlyReset;           // true = sick leave wipes at month end

    @Column(nullable = false)
    private int maxCarryForward;            // 30 for carry-forwardable types

    @Column(nullable = false)
    private boolean genderRestricted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Gender genderRestriction;       // null = open to all

    // --- getters & setters ---
    public Integer getId() { return id; }
    public String getName() { return name; }
    public boolean isCarriesForward() { return carriesForward; }
    public boolean isMonthlyReset() { return monthlyReset; }
    public int getMaxCarryForward() { return maxCarryForward; }
    public boolean isGenderRestricted() { return genderRestricted; }
    public Gender getGenderRestriction() { return genderRestriction; }

    public void setId(Integer id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCarriesForward(boolean cf) { this.carriesForward = cf; }
    public void setMonthlyReset(boolean mr) { this.monthlyReset = mr; }
    public void setMaxCarryForward(int max) { this.maxCarryForward = max; }
    public void setGenderRestricted(boolean gr) { this.genderRestricted = gr; }
    public void setGenderRestriction(Gender g) { this.genderRestriction = g; }
}