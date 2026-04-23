package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;

@Service
public class MealSlotService {

    private final Map<MealSlot, LocalTime> slotLocalTimeMap = Map.of(
            MealSlot.BREAKFAST, LocalTime.of(8, 0),
            MealSlot.LUNCH, LocalTime.of(13, 0),
            MealSlot.DINNER, LocalTime.of(21, 0)
    );

    public LocalTime getTime(MealSlot slot) {
        return slotLocalTimeMap.get(slot);
    }
}
