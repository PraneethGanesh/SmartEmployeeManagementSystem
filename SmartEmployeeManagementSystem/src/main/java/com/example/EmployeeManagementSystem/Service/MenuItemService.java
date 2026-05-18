package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.MenuItemDTO;
import com.example.EmployeeManagementSystem.DTO.MenuItemRequest;
import com.example.EmployeeManagementSystem.Entity.MenuItem;
import com.example.EmployeeManagementSystem.Entity.Restaurant;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Exception.RestaurantNotFoundException;
import com.example.EmployeeManagementSystem.Repository.MenuItemRepository;
import com.example.EmployeeManagementSystem.Repository.RestaurantRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemService(MenuItemRepository menuItemRepository,
                           RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    /** Vendor adds a menu item to their restaurant */
    public MenuItemDTO addMenuItem(MenuItemRequest request, Authentication authentication) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant with id " + request.getRestaurantId() + " not found"));

        // Verify the vendor owns this restaurant
        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!restaurant.getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }

        // Validate restaurant supports this meal slot
        if (!restaurant.getSupportedMealSlots().contains(request.getMealSlot())) {
            throw new IllegalArgumentException(
                    "Restaurant does not support meal slot: " + request.getMealSlot());
        }

        MenuItem item = new MenuItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setMealSlot(request.getMealSlot());
        item.setRestaurant(restaurant);
        item.setAvailable(true);

        return toDTO(menuItemRepository.save(item));
    }

    /** Vendor updates a menu item */
    public MenuItemDTO updateMenuItem(Long itemId, MenuItemRequest request, Authentication authentication) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemId));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!item.getRestaurant().getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this menu item");
        }

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setMealSlot(request.getMealSlot());

        return toDTO(menuItemRepository.save(item));
    }

    /** Vendor toggles availability of a menu item */
    public MenuItemDTO toggleAvailability(Long itemId, Authentication authentication) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemId));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!item.getRestaurant().getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this menu item");
        }

        item.setAvailable(!item.isAvailable());
        return toDTO(menuItemRepository.save(item));
    }

    /** Vendor deletes a menu item */
    public void deleteMenuItem(Long itemId, Authentication authentication) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemId));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!item.getRestaurant().getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this menu item");
        }

        menuItemRepository.delete(item);
    }

    /** Employee views available menu items for a restaurant + meal slot */
    public List<MenuItemDTO> getMenuByRestaurantAndSlot(Long restaurantId, MealSlot mealSlot) {
        return menuItemRepository
                .findByRestaurantIdAndMealSlotAndAvailableTrue(restaurantId, mealSlot)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Vendor views all menu items for their restaurant */
    public List<MenuItemDTO> getAllItemsForRestaurant(Long restaurantId, Authentication authentication) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant with id " + restaurantId + " not found"));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!restaurant.getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }

        return menuItemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private MenuItemDTO toDTO(MenuItem item) {
        MenuItemDTO dto = new MenuItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setAvailable(item.isAvailable());
        dto.setMealSlot(item.getMealSlot());
        dto.setRestaurantId(item.getRestaurant().getId());
        dto.setRestaurantName(item.getRestaurant().getName());
        return dto;
    }
}
