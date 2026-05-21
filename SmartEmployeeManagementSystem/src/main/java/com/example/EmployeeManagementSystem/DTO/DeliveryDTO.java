package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.DeliveryStatus;
import com.example.EmployeeManagementSystem.Enum.MealSlot;

import java.time.Instant;

public class DeliveryDTO {
    private Long id;
    private Long subscriptionId;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeeDept;
    private Long restaurantId;
    private String restaurantName;
    private MealSlot mealSlot;
    private Instant scheduledDeliveryTime;
    private Instant actualDeliveryTime;
    private DeliveryStatus status;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public String getEmployeeDept() { return employeeDept; }
    public void setEmployeeDept(String employeeDept) { this.employeeDept = employeeDept; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public MealSlot getMealSlot() { return mealSlot; }
    public void setMealSlot(MealSlot mealSlot) { this.mealSlot = mealSlot; }

    public Instant getScheduledDeliveryTime() { return scheduledDeliveryTime; }
    public void setScheduledDeliveryTime(Instant scheduledDeliveryTime) { this.scheduledDeliveryTime = scheduledDeliveryTime; }

    public Instant getActualDeliveryTime() { return actualDeliveryTime; }
    public void setActualDeliveryTime(Instant actualDeliveryTime) { this.actualDeliveryTime = actualDeliveryTime; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
