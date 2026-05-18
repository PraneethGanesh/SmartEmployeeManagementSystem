package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.MealOrder;
import com.example.EmployeeManagementSystem.Enum.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealOrderRepository extends JpaRepository<MealOrder, Long> {

    List<MealOrder> findByEmployeeEmployeeIdOrderByPlacedAtDesc(long employeeId);

    List<MealOrder> findBySubscriptionIdOrderByPlacedAtDesc(Long subscriptionId);

    List<MealOrder> findBySubscriptionRestaurantVendorIdAndStatus(Long vendorId, OrderStatus status);

    List<MealOrder> findBySubscriptionRestaurantVendorId(Long vendorId);
}