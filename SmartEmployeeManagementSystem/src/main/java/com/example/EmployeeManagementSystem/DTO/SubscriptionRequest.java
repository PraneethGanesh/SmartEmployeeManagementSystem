package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Enum.ScheduleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;

public class SubscriptionRequest {
    private ScheduleType scheduleType;
    private DayOfWeek dayOfWeek;
    @NotNull
    @NotEmpty
    private List<MealSlot> mealSlots;
    @NotNull
    private List<SlotOrder> slotOrders;
    public static class SlotOrder {
        @NotNull
        private MealSlot mealSlot;

        @NotNull
        private Long restaurantId;

        public MealSlot getMealSlot() {
            return mealSlot;
        }

        public void setMealSlot(MealSlot mealSlot) {
            this.mealSlot = mealSlot;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public List<MealSlot> getMealSlots() {
        return mealSlots;
    }

    public void setMealSlots(List<MealSlot> mealSlots) {
        this.mealSlots = mealSlots;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }



    public List<SlotOrder> getSlotOrders() { return slotOrders; }
    public void setSlotOrders(List<SlotOrder> slotOrders) { this.slotOrders = slotOrders; }
}
