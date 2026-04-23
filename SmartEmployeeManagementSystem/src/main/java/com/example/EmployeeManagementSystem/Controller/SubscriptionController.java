package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.SubscriptionDTO;
import com.example.EmployeeManagementSystem.DTO.SubscriptionRequest;
import com.example.EmployeeManagementSystem.Entity.Subscription;
import com.example.EmployeeManagementSystem.Service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    //Authentication makes sure that whoever is logged can add subscription and cannot add other subscription
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<?> addSubscription(@RequestBody SubscriptionRequest request,Authentication authentication) {
        subscriptionService.addSubscription(request,authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Added new subscription for user: " + authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public List<SubscriptionDTO> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<Subscription> updateSubscription(
            @PathVariable Long id,
            @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.updateSubscription(id, request));
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<Subscription> pauseSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.pauseSubscription(id));
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<Subscription> resumeSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.resumeSubscription(id));
    }

    @PutMapping("/{id}/expire")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<Subscription> expireSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.expireSubscription(id));
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<List<SubscriptionDTO>> getSubscriptionOfUser(Authentication authentication){
        return ResponseEntity.ok(subscriptionService.getSubscriptionOfUser(authentication));
    }

}
