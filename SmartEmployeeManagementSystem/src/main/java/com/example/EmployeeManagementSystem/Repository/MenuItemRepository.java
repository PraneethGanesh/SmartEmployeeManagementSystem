package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.MenuItem;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantIdAndMealSlotAndAvailableTrue(Long restaurantId, MealSlot mealSlot);

    List<MenuItem> findByRestaurantId(Long restaurantId);
}
