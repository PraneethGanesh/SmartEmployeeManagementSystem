package com.example.EmployeeManagementSystem.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_order_id", nullable = false)
    private MealOrder mealOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    private int quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MealOrder getMealOrder() { return mealOrder; }
    public void setMealOrder(MealOrder mealOrder) { this.mealOrder = mealOrder; }

    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}