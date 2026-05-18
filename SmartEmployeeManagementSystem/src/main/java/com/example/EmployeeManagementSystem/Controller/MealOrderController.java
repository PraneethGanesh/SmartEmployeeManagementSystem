package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.MealOrderDTO;
import com.example.EmployeeManagementSystem.DTO.MealOrderRequest;
import com.example.EmployeeManagementSystem.Service.MealOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Meal order endpoints.
 *
 * ── EMPLOYEE / MANAGER / ADMIN (place & view) ─────────────────────────────────
 * POST   /orders                    — Place an order for an active subscription
 * GET    /orders/my                 — View my own order history
 *
 * ── VENDOR (manage & notify) ─────────────────────────────────────────────────
 * GET    /orders/vendor             — View all orders for vendor's restaurants
 * GET    /orders/vendor/pending     — View only PLACED orders needing action
 * PUT    /orders/{id}/confirm       — Confirm order (vendor ordered from external service)
 * PUT    /orders/{id}/arrived       — Mark order as arrived → notifies employee
 */
@RestController
@RequestMapping("/orders")
public class MealOrderController {

    private final MealOrderService mealOrderService;

    public MealOrderController(MealOrderService mealOrderService) {
        this.mealOrderService = mealOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<MealOrderDTO> placeOrder(@Valid @RequestBody MealOrderRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealOrderService.placeOrder(request, authentication));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<List<MealOrderDTO>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(mealOrderService.getMyOrders(authentication));
    }

    @GetMapping("/vendor")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<List<MealOrderDTO>> getVendorOrders(Authentication authentication) {
        return ResponseEntity.ok(mealOrderService.getVendorOrders(authentication));
    }

    @GetMapping("/vendor/pending")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<List<MealOrderDTO>> getVendorPendingOrders(Authentication authentication) {
        return ResponseEntity.ok(mealOrderService.getVendorPendingOrders(authentication));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<MealOrderDTO> confirmOrder(@PathVariable Long id,
                                                     Authentication authentication) {
        return ResponseEntity.ok(mealOrderService.confirmOrder(id, authentication));
    }

    @PutMapping("/{id}/arrived")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<MealOrderDTO> markArrived(@PathVariable Long id,
                                                    Authentication authentication) {
        return ResponseEntity.ok(mealOrderService.markArrived(id, authentication));
    }
}
