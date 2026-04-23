package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class RestaurantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    private String description;

    /**
     * Which meal slots this restaurant serves.
     * Must be at least one of: BREAKFAST, LUNCH, DINNER.
     */
    @NotNull
    @NotEmpty
    private Set<MealSlot> supportedMealSlots;

    /**
     * The vendor ID of the owner making this request.
     * Used to verify ownership on updates/deletes.
     */

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<MealSlot> getSupportedMealSlots() { return supportedMealSlots; }
    public void setSupportedMealSlots(Set<MealSlot> supportedMealSlots) {
        this.supportedMealSlots = supportedMealSlots;
    }
}