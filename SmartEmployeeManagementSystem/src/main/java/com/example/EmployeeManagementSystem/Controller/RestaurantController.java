package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.RestaurantDTO;
import com.example.EmployeeManagementSystem.DTO.RestaurantRequest;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Service.RestaurantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Restaurant management endpoints.
 *
 * ── VENDOR-ONLY (write operations) ───────────────────────────────────────────
 * POST   /restaurants                        — Create a restaurant (vendor supplies vendorId in body)
 * PUT    /restaurants/{id}                   — Update restaurant details (vendor ownership verified)
 * PUT    /restaurants/{id}/deactivate        — Soft-delete a restaurant  (vendor ownership verified)
 * GET    /restaurants/vendor/{vendorId}      — List MY restaurants (vendor views their own)
 *
 * ── EMPLOYEE / MANAGER (read-only browse) ────────────────────────────────────
 * GET    /restaurants                        — Browse all active restaurants
 * GET    /restaurants/{id}                   — Get a specific restaurant
 * GET    /restaurants/by-slot?slot=LUNCH     — Find restaurants that serve a given meal slot
 *
 * Employees use the browse endpoints to pick a restaurant when creating a subscription.
 * Employees and managers cannot create, update, or deactivate restaurants.
 */
@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    // ── VENDOR-ONLY write operations ─────────────────────────────────────────

    /**
     * Create a new restaurant.
     * Request body must include vendorId — the service validates the vendor exists.
     */
    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<RestaurantDTO> createRestaurant(@RequestBody RestaurantRequest request,Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(request,authentication));
    }

    /**
     * Update a restaurant's name, address, description, or supported meal slots.
     * vendorId in the request body is used to verify ownership before any change is made.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<RestaurantDTO> updateRestaurant(@PathVariable Long id,
                                                          @RequestBody RestaurantRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request,authentication));
    }

    /**
     * Soft-deactivate a restaurant. Active subscriptions remain in the DB but
     * new deliveries will stop since the restaurant is inactive.
     * Pass vendorId as a query param so ownership can be verified.
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> deactivateRestaurant(@PathVariable Long id,
                                                       Authentication authentication) {
        restaurantService.deactivateRestaurant(id, authentication);
        return ResponseEntity.ok("Restaurant " + id + " has been deactivated.");
    }

    /**
     * List all active restaurants owned by a specific vendor.
     * Only the vendor themselves should call this to see their portfolio.
     */
    @GetMapping("/vendor")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<RestaurantDTO>> getRestaurantsByVendor(Authentication authentication) {
        return ResponseEntity.ok(restaurantService.getRestaurantsByVendor(authentication));
    }

    // ── EMPLOYEE / MANAGER read-only browse ──────────────────────────────────

    /**
     * Browse all active restaurants.
     * Employees use this to discover available restaurants before subscribing.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('RESTAURANTS_READ')")
    public ResponseEntity<List<RestaurantDTO>> getAllActiveRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants());
    }

    /**
     * Get a specific restaurant by id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('RESTAURANTS_READ')")
    public ResponseEntity<RestaurantDTO> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurant(id));
    }

    /**
     * Find all active restaurants that serve a specific meal slot.
     * Example: GET /restaurants/by-slot?slot=LUNCH
     * Employees call this when building their subscription to see valid options for each slot.
     */
    @GetMapping("/by-slot")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR','EMPLOYEE','MANAGER')")
    public ResponseEntity<List<RestaurantDTO>> getRestaurantsByMealSlot(@RequestParam MealSlot slot) {
        return ResponseEntity.ok(restaurantService.getRestaurantsByMealSlot(slot));
    }

}
