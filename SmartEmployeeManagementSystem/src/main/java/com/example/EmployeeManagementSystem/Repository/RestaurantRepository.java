package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Restaurant;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** All active restaurants owned by a vendor. */
    List<Restaurant> findByVendor_EmailAndActiveTrue(String email);

    /** All active restaurants (for employees browsing available options). */
    List<Restaurant> findByActiveTrue();

    /**
     * Active restaurants that support a given meal slot.
     * Employees use this to discover which restaurants they can subscribe to for a given slot.
     */
    @Query("SELECT r FROM Restaurant r JOIN r.supportedMealSlots s " +
            "WHERE r.active = true AND s = :mealSlot")
    List<Restaurant> findActiveByMealSlot(@Param("mealSlot") MealSlot mealSlot);

    /** Check if a restaurant belongs to a vendor (ownership guard). */
    boolean existsByIdAndVendor_Email(Long restaurantId, String email);
}