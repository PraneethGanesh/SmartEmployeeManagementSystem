package com.example.EmployeeManagementSystem.DTO;
import com.example.EmployeeManagementSystem.Enum.MealSlot;

public class MenuItemDTO {

    private Long id;
    private String name;
    private String description;
    private double price;
    private boolean available;
    private MealSlot mealSlot;
    private Long restaurantId;
    private String restaurantName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public MealSlot getMealSlot() { return mealSlot; }
    public void setMealSlot(MealSlot mealSlot) { this.mealSlot = mealSlot; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
}