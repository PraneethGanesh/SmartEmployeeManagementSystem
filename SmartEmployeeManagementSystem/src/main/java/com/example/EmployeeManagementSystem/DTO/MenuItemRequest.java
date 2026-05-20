package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MenuItemRequest {

    @NotBlank
    private String name;

    private String description;

    @Positive
    private double price;

    @NotNull
    private MealSlot mealSlot;

    @NotNull
    private Long restaurantId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public MealSlot getMealSlot() { return mealSlot; }
    public void setMealSlot(MealSlot mealSlot) { this.mealSlot = mealSlot; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
}