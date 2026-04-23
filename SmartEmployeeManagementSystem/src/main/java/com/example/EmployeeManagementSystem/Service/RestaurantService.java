package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.RestaurantDTO;
import com.example.EmployeeManagementSystem.DTO.RestaurantRequest;
import com.example.EmployeeManagementSystem.Entity.Restaurant;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Exception.RestaurantNotFoundException;
import com.example.EmployeeManagementSystem.Exception.UnauthorizedAccessException;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.RestaurantRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final VendorRepo vendorRepo;

    public RestaurantService(RestaurantRepository restaurantRepository, VendorRepo vendorRepo) {
        this.restaurantRepository = restaurantRepository;
        this.vendorRepo = vendorRepo;
    }

    /**
     * Create a new restaurant.
     * Only the owning vendor (matched by vendorId in the request) may do this.
     */
    public RestaurantDTO createRestaurant(RestaurantRequest request,Authentication authentication) {
        Vendor vendor=vendorRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new VendorNotFoundException(
                        "Vendor with email " + authentication.getName() + " not found"));

        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setAddress(request.getAddress());
        restaurant.setDescription(request.getDescription());
        restaurant.setSupportedMealSlots(request.getSupportedMealSlots());
        restaurant.setVendor(vendor);
        restaurant.setActive(true);

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created: {} by vendor {}", saved.getName(), vendor.getId());
        return toDTO(saved);
    }

    /**
     * Update restaurant details.
     * Vendor must own this restaurant — otherwise 403.
     */
    public RestaurantDTO updateRestaurant(Long restaurantId, RestaurantRequest request,Authentication authentication) {
        Restaurant restaurant = findOrThrow(restaurantId);
        Vendor vendor=vendorRepo.findByEmail(authentication.getName()).orElseThrow(
                ()->new VendorNotFoundException("Vendor Not found")
        );
        assertOwnership(restaurantId, vendor.getEmail());

        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getAddress() != null) restaurant.setAddress(request.getAddress());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getSupportedMealSlots() != null && !request.getSupportedMealSlots().isEmpty()) {
            restaurant.setSupportedMealSlots(request.getSupportedMealSlots());
        }
        return toDTO(restaurantRepository.save(restaurant));
    }

    /**
     * Deactivate a restaurant (soft delete).
     * Only the owning vendor may do this.
     * Active subscriptions to this restaurant will no longer receive new deliveries
     * — the Subscription status should be handled separately.
     */
    public void deactivateRestaurant(Long restaurantId, Authentication authentication) {
        Restaurant restaurant = findOrThrow(restaurantId);
        Vendor vendor=vendorRepo.findByEmail(authentication.getName()).orElseThrow(
                ()->new VendorNotFoundException("Vendor Not found")
        );
        assertOwnership(restaurantId, vendor.getEmail());
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
        log.info("Restaurant deactivated: id={}", restaurantId);
    }

    /**
     * Get all restaurants owned by a vendor.
     * VENDOR-only endpoint.
     */
    public List<RestaurantDTO> getRestaurantsByVendor(Authentication authentication) {
        vendorRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new VendorNotFoundException("Vendor " + authentication.getName() + " not found"));
        return restaurantRepository.findByVendor_EmailAndActiveTrue(authentication.getName())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all active restaurants.
     * Available to EMPLOYEE so they can browse and choose.
     */
    public List<RestaurantDTO> getAllActiveRestaurants() {
        return restaurantRepository.findByActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get active restaurants that serve a specific meal slot.
     * Employees call this when subscribing, to see valid restaurant options for a slot.
     */
    public List<RestaurantDTO> getRestaurantsByMealSlot(MealSlot mealSlot) {
        return restaurantRepository.findActiveByMealSlot(mealSlot)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public RestaurantDTO getRestaurant(Long id) {
        return toDTO(findOrThrow(id));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Restaurant findOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant with id " + id + " not found"));
    }

    /**
     * Throws UnauthorizedAccessException if the restaurant does not belong to the vendor.
     * This is the core access-control guard for all mutating vendor operations.
     */
    private void assertOwnership(Long restaurantId, String email) {
        if (!restaurantRepository.existsByIdAndVendor_Email(restaurantId, email)) {
            throw new UnauthorizedAccessException(
                    "Vendor " + email + " does not own restaurant " + restaurantId);
        }
    }

    public RestaurantDTO toDTO(Restaurant r) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setAddress(r.getAddress());
        dto.setDescription(r.getDescription());
        dto.setSupportedMealSlots(r.getSupportedMealSlots());
        dto.setActive(r.isActive());
        dto.setVendorId(r.getVendor().getId());
        dto.setVendorName(r.getVendor().getName());
        return dto;
    }
}