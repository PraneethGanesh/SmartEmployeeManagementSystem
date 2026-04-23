package com.example.EmployeeManagementSystem.Repository;

import com.example.EmployeeManagementSystem.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    @Query(value = "select count(*) from subscription s where s.employee_id=:employeeId and s.slot=:slot and s.status=:status",nativeQuery = true)
    int checkSubscriptionExists(
            @Param("employeeId") long employeeId,
            @Param("slot") String slot,
            @Param("status") String status
    );

    List<Subscription> findByEmployee_employeeId(long id);

    @Query(value = "SELECT * FROM subscription s WHERE s.status = :status AND s.next_delivery_time IS NOT NULL AND s.next_delivery_time <= :currentTime", nativeQuery = true)
    List<Subscription> findDueSubscriptions(
            @Param("status") String status,
            @Param("currentTime") Instant currentTime  // ← was LocalDateTime
    );

    List<Subscription> findByEmployee_name(String name);
    List<Subscription> findByEmployee_Email(String email);
}
