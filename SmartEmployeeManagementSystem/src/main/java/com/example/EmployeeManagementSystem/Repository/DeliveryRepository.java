package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Delivery;
import com.example.EmployeeManagementSystem.Entity.Subscription;
import com.example.EmployeeManagementSystem.Enum.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery,Long> {
    List<Delivery> findByStatusAndScheduledDeliveryTimeLessThanEqual(
            DeliveryStatus status, Instant time);

    @Query("SELECT d FROM Delivery d WHERE d.subscription = :subscription " +
            "AND d.status != :status ORDER BY d.scheduledDeliveryTime DESC")
    List<Delivery> findRecentDeliveriesBySubscription(
            @Param("subscription") Subscription subscription,
            @Param("status") DeliveryStatus status);
}
