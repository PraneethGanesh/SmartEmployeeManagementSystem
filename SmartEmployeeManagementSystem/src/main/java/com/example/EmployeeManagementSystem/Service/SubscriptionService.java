package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.SubscriptionDTO;
import com.example.EmployeeManagementSystem.DTO.SubscriptionRequest;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Restaurant;
import com.example.EmployeeManagementSystem.Entity.Subscription;
import com.example.EmployeeManagementSystem.Enum.MealSlot;
import com.example.EmployeeManagementSystem.Enum.ScheduleType;
import com.example.EmployeeManagementSystem.Enum.SubscriptionStatus;
import com.example.EmployeeManagementSystem.Exception.EmployeeNotFound;
import com.example.EmployeeManagementSystem.Exception.RestaurantNotFoundException;
import com.example.EmployeeManagementSystem.Exception.SubscriptionAlreadyExists;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RestaurantRepository;
import com.example.EmployeeManagementSystem.Repository.SubscriptionRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EmployeeRepo employeeRepo;
    private final DeliveryTimeService deliveryTimeService;
    private final RestaurantRepository restaurantRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               EmployeeRepo employeeRepo,
                               DeliveryTimeService deliveryTimeService,
                               RestaurantRepository restaurantRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.employeeRepo = employeeRepo;
        this.deliveryTimeService = deliveryTimeService;
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Add subscriptions for an employee.
     *
     * Each SlotOrder specifies a meal slot AND the restaurant for that slot,
     * so an employee can have breakfast from Restaurant A and lunch from Restaurant B.
     *
     * Validations:
     * - Employee must exist and be ACTIVE
     * - Restaurant must exist and be active
     * - Restaurant must support the requested meal slot
     * - No duplicate active subscription for the same employee + slot combination
     */
    public void addSubscription(SubscriptionRequest request,Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFound(
                        "Employee with id " + email + " not found"));

        if (request.getSlotOrders() == null || request.getSlotOrders().isEmpty()) {
            throw new IllegalArgumentException("slotOrders cannot be null or empty");
        }

        if (request.getScheduleType() == ScheduleType.DAILY) {
            request.setDayOfWeek(null);
        } else if (request.getScheduleType() == ScheduleType.WEEKLY) {
            if (request.getDayOfWeek() == null) {
                throw new IllegalArgumentException("dayOfWeek is required for WEEKLY schedule");
            }
        }

        for (SubscriptionRequest.SlotOrder slotOrder : request.getSlotOrders()) {
            MealSlot slot = slotOrder.getMealSlot();
            Long restaurantId = slotOrder.getRestaurantId();

            // 1. Validate restaurant exists and is active
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new RestaurantNotFoundException(
                            "Restaurant with id " + restaurantId + " not found"));
            if (!restaurant.isActive()) {
                throw new IllegalArgumentException(
                        "Restaurant '" + restaurant.getName() + "' is not currently active");
            }

            // 2. Validate the restaurant actually serves this meal slot
            if (!restaurant.getSupportedMealSlots().contains(slot)) {
                throw new IllegalArgumentException(
                        "Restaurant '" + restaurant.getName() + "' does not serve " + slot +
                                ". Supported slots: " + restaurant.getSupportedMealSlots());
            }

            // 3. Check for duplicate active subscription (same employee + slot)
            if (subscriptionRepository.checkSubscriptionExists(
                    employee.getEmployeeId(), slot.name(), SubscriptionStatus.ACTIVE.name()) > 0) {
                throw new SubscriptionAlreadyExists(
                        "Employee " + employee.getEmployeeId() +
                                " already has an active " + slot + " subscription. " +
                                "Cancel the existing one before subscribing to a new restaurant.");
            }

            // 4. Create subscription
            Subscription subscription = new Subscription();
            subscription.setEmployee(employee);
            subscription.setSlot(slot);
            subscription.setRestaurant(restaurant);
            subscription.setScheduleType(request.getScheduleType());
            subscription.setDayOfWeek(request.getDayOfWeek());
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setNextDeliveryTime(
                    deliveryTimeService.calculateNextDelivery(request, slot, employee));

            subscriptionRepository.save(subscription);
        }
    }

    public List<SubscriptionDTO> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<SubscriptionDTO> dtoList = new ArrayList<>();
        for (Subscription s : subscriptions) {
            dtoList.add(convertToDTO(s));
        }
        return dtoList;
    }

    public Subscription updateSubscription(Long id, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        Employee employee = subscription.getEmployee();

        if (request.getScheduleType() == ScheduleType.DAILY) {
            subscription.setDayOfWeek(null);
        } else if (request.getScheduleType() == ScheduleType.WEEKLY) {
            if (request.getDayOfWeek() == null) {
                throw new IllegalArgumentException("dayOfWeek is required for WEEKLY schedule");
            }
            subscription.setDayOfWeek(request.getDayOfWeek());
        }
        subscription.setScheduleType(request.getScheduleType());

        // Update slot & restaurant if a single slotOrder is provided
        if (request.getSlotOrders() != null && !request.getSlotOrders().isEmpty()) {
            if (request.getSlotOrders().size() > 1) {
                throw new IllegalArgumentException(
                        "Update supports one slot at a time. Use separate subscriptions for multiple slots.");
            }
            SubscriptionRequest.SlotOrder slotOrder = request.getSlotOrders().get(0);
            MealSlot newSlot = slotOrder.getMealSlot();
            Restaurant restaurant = restaurantRepository.findById(slotOrder.getRestaurantId())
                    .orElseThrow(() -> new RestaurantNotFoundException(
                            "Restaurant " + slotOrder.getRestaurantId() + " not found"));
            if (!restaurant.isActive()) {
                throw new IllegalArgumentException("Restaurant '" + restaurant.getName() + "' is not active");
            }
            if (!restaurant.getSupportedMealSlots().contains(newSlot)) {
                throw new IllegalArgumentException(
                        "Restaurant '" + restaurant.getName() + "' does not serve " + newSlot);
            }
            subscription.setSlot(newSlot);
            subscription.setRestaurant(restaurant);
        }

        subscription.setNextDeliveryTime(
                deliveryTimeService.calculateNextDelivery(request, subscription.getSlot(), employee));

        return subscriptionRepository.save(subscription);
    }

    public Subscription pauseSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription.setNextDeliveryTime(null);
        return subscriptionRepository.save(subscription);
    }

    public Subscription resumeSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setNextDeliveryTime(deliveryTimeService.getNextDeliveryTime(subscription));
        return subscriptionRepository.save(subscription);
    }

    public Subscription expireSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscription.setNextDeliveryTime(null);
        return subscriptionRepository.save(subscription);
    }

    public List<SubscriptionDTO> getSubscriptionOfUser(Authentication authentication) {
        String email= AuthUtil.extractEmail(authentication);
        List<Subscription> subscriptions = subscriptionRepository.findByEmployee_Email(email);
        List<SubscriptionDTO> dtoList = new ArrayList<>();
        for (Subscription s : subscriptions) {
            dtoList.add(convertToDTO(s));
        }
        return dtoList;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SubscriptionDTO convertToDTO(Subscription s) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(s.getId());
        dto.setUserId(s.getEmployee().getEmployeeId());
        dto.setMealSlot(s.getSlot());
        dto.setScheduleType(s.getScheduleType());
        if (s.getScheduleType() == ScheduleType.WEEKLY) {
            dto.setDayOfWeek(s.getDayOfWeek());
        }
        dto.setStatus(s.getStatus());
        if (s.getNextDeliveryTime() != null) {
            ZoneId userZone = ZoneId.of(s.getEmployee().getTimezone());
            dto.setNextDeliveryTime(s.getNextDeliveryTime().atZone(userZone));
        }
        // Restaurant info
        if (s.getRestaurant() != null) {
            dto.setRestaurantId(s.getRestaurant().getId());
            dto.setRestaurantName(s.getRestaurant().getName());
            dto.setRestaurantAddress(s.getRestaurant().getAddress());
        }
        return dto;
    }
}