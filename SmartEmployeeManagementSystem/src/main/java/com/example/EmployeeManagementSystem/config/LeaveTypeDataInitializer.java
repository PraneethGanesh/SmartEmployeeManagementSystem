package com.example.EmployeeManagementSystem.config;

import com.example.EmployeeManagementSystem.Entity.LeaveType;
import com.example.EmployeeManagementSystem.Enum.Gender;
import com.example.EmployeeManagementSystem.Repository.LeaveTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LeaveTypeDataInitializer implements CommandLineRunner {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeDataInitializer(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    public void run(String... args) {
        ensureLeaveType("SICK", false, true, 0, false, null);
        ensureLeaveType("CASUAL", true, false, 30, false, null);
        ensureLeaveType("MATERNITY", true, false, 30, true, Gender.F);
    }

    private void ensureLeaveType(String name,
                                 boolean carriesForward,
                                 boolean monthlyReset,
                                 int maxCarryForward,
                                 boolean genderRestricted,
                                 Gender genderRestriction) {
        leaveTypeRepository.findByName(name).orElseGet(() -> {
            LeaveType leaveType = new LeaveType();
            leaveType.setName(name);
            leaveType.setCarriesForward(carriesForward);
            leaveType.setMonthlyReset(monthlyReset);
            leaveType.setMaxCarryForward(maxCarryForward);
            leaveType.setGenderRestricted(genderRestricted);
            leaveType.setGenderRestriction(genderRestriction);
            return leaveTypeRepository.save(leaveType);
        });
    }
}
