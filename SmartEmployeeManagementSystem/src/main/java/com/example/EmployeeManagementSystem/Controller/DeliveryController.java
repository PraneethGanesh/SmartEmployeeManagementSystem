package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.DeliveryDTO;
import com.example.EmployeeManagementSystem.Enum.DeliveryStatus;
import com.example.EmployeeManagementSystem.Service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryDTO> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam DeliveryStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, status, authentication));
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<DeliveryDTO>> getDeliveriesBySubscription(
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesBySubscription(subscriptionId));
    }

    @PutMapping("/{id}/delivered")
    public ResponseEntity<DeliveryDTO> markDelivered(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(id, DeliveryStatus.DELIVERED, authentication));
    }
    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }
}
