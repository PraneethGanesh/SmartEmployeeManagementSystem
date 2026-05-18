package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Enum.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class MealOrderDTO {

    private Long id;
    private Long subscriptionId;
    private Long employeeId;
    private String employeeName;
    private MealSlot mealSlot;
    private OrderStatus status;
    private LocalDateTime placedAt;
    private LocalDateTime deliveredAt;
    private String restaurantName;
    private List<OrderItemDTO> items;

    public static class OrderItemDTO {
        private Long menuItemId;
        private String menuItemName;
        private double price;
        private int quantity;

        public Long getMenuItemId() { return menuItemId; }
        public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

        public String getMenuItemName() { return menuItemName; }
        public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public MealSlot getMealSlot() { return mealSlot; }
    public void setMealSlot(MealSlot mealSlot) { this.mealSlot = mealSlot; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }
}