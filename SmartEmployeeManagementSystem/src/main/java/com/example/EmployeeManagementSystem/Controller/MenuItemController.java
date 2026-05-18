package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.MenuItemDTO;
import com.example.EmployeeManagementSystem.DTO.MenuItemRequest;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Menu item management endpoints.
 *
 * ── VENDOR-ONLY (write operations) ───────────────────────────────────────────
 * POST   /menu-items                              — Add a menu item to a restaurant
 * PUT    /menu-items/{id}                         — Update a menu item
 * PUT    /menu-items/{id}/toggle-availability     — Enable or disable a menu item
 * DELETE /menu-items/{id}                         — Delete a menu item
 * GET    /menu-items/restaurant/{id}/all          — View all items for their restaurant
 *
 * ── EMPLOYEE / MANAGER (read-only) ───────────────────────────────────────────
 * GET    /menu-items/restaurant/{id}?slot=LUNCH   — View available items by restaurant + slot
 */
@RestController
@RequestMapping("/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<MenuItemDTO> addMenuItem(@Valid @RequestBody MenuItemRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemService.addMenuItem(request, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<MenuItemDTO> updateMenuItem(@PathVariable Long id,
                                                      @Valid @RequestBody MenuItemRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(menuItemService.updateMenuItem(id, request, authentication));
    }

    @PutMapping("/{id}/toggle-availability")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<MenuItemDTO> toggleAvailability(@PathVariable Long id,
                                                          Authentication authentication) {
        return ResponseEntity.ok(menuItemService.toggleAvailability(id, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<String> deleteMenuItem(@PathVariable Long id,
                                                 Authentication authentication) {
        menuItemService.deleteMenuItem(id, authentication);
        return ResponseEntity.ok("Menu item " + id + " deleted successfully");
    }

    @GetMapping("/restaurant/{restaurantId}/all")
    @PreAuthorize("hasRole('FOOD_VENDOR')")
    public ResponseEntity<List<MenuItemDTO>> getAllItemsForRestaurant(@PathVariable Long restaurantId,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(menuItemService.getAllItemsForRestaurant(restaurantId, authentication));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public ResponseEntity<List<MenuItemDTO>> getMenuByRestaurantAndSlot(@PathVariable Long restaurantId,
                                                                        @RequestParam MealSlot slot) {
        return ResponseEntity.ok(menuItemService.getMenuByRestaurantAndSlot(restaurantId, slot));
    }
}
