package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MealSlot;

import java.util.Set;

public class RestaurantDTO {
    private Long id;
    private String name;
    private String address;
    private String description;
    private Set<MealSlot> supportedMealSlots;
    private boolean active;
    private Long vendorId;
    private String vendorName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
}