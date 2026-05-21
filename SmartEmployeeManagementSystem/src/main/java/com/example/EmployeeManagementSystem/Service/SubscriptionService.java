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
import com.example.EmployeeManagementSystem.Repository.DeliveryRepository;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.RestaurantRepository;
import com.example.EmployeeManagementSystem.Repository.SubscriptionRepository;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DeliveryRepository deliveryRepository;
    private final OperationLogService operationLogService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               EmployeeRepo employeeRepo,
                               DeliveryTimeService deliveryTimeService,
                               RestaurantRepository restaurantRepository,
                               DeliveryRepository deliveryRepository,
                               OperationLogService operationLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.employeeRepo = employeeRepo;
        this.deliveryTimeService = deliveryTimeService;
        this.restaurantRepository = restaurantRepository;
        this.deliveryRepository = deliveryRepository;
        this.operationLogService = operationLogService;
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

            Subscription saved = subscriptionRepository.save(subscription);
            operationLogService.record(authentication, "SUBSCRIPTION", "Subscription", saved.getId(),
                    "CREATE_SUBSCRIPTION", null, saved.getStatus(),
                    "Subscription created for " + slot + " at " + restaurant.getName());
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

    public Subscription updateSubscription(Long id, SubscriptionRequest request, Authentication authentication) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        SubscriptionStatus previousStatus = subscription.getStatus();
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

        Subscription saved = subscriptionRepository.save(subscription);
        operationLogService.record(authentication, "SUBSCRIPTION", "Subscription", saved.getId(),
                "UPDATE_SUBSCRIPTION", previousStatus, saved.getStatus(),
                "Subscription schedule updated for employee " + employee.getName());
        return saved;
    }

    public Subscription pauseSubscription(Long id, Authentication authentication) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription.setNextDeliveryTime(null);
        Subscription saved = subscriptionRepository.save(subscription);
        operationLogService.record(authentication, "SUBSCRIPTION", "Subscription", saved.getId(),
                "PAUSE_SUBSCRIPTION", previousStatus, saved.getStatus(),
                "Subscription paused for employee " + saved.getEmployee().getName());
        return saved;
    }

    public Subscription resumeSubscription(Long id, Authentication authentication) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setNextDeliveryTime(deliveryTimeService.getNextDeliveryTime(subscription));
        Subscription saved = subscriptionRepository.save(subscription);
        operationLogService.record(authentication, "SUBSCRIPTION", "Subscription", saved.getId(),
                "RESUME_SUBSCRIPTION", previousStatus, saved.getStatus(),
                "Subscription resumed for employee " + saved.getEmployee().getName());
        return saved;
    }

    public Subscription expireSubscription(Long id, Authentication authentication) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscription.setNextDeliveryTime(null);
        Subscription saved = subscriptionRepository.save(subscription);
        operationLogService.record(authentication, "SUBSCRIPTION", "Subscription", saved.getId(),
                "EXPIRE_SUBSCRIPTION", previousStatus, saved.getStatus(),
                "Subscription expired for employee " + saved.getEmployee().getName());
        return saved;
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

    @Transactional
    public void deleteSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        SubscriptionStatus previousStatus = subscription.getStatus();
        deliveryRepository.deleteBySubscription(subscription);
        subscriptionRepository.delete(subscription);
        operationLogService.recordSystem("SUBSCRIPTION", "Subscription", id,
                "DELETE_SUBSCRIPTION", previousStatus, "DELETED",
                "Subscription deleted for employee " + subscription.getEmployee().getName());
    }

    @Transactional
    public void deleteSubscriptionsByEmployeeEmail(String email) {
        List<Subscription> subscriptions = subscriptionRepository.findByEmployee_Email(email);
        if (subscriptions.isEmpty()) {
            return;
        }
        deliveryRepository.deleteBySubscriptionIn(subscriptions);
        subscriptionRepository.deleteAll(subscriptions);
    }

    private SubscriptionDTO convertToDTO(Subscription s) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(s.getId());
        dto.setUserId(s.getEmployee().getEmployeeId());
        dto.setEmployeeName(s.getEmployee().getName());
        dto.setEmployeeEmail(s.getEmployee().getEmail());
        dto.setEmployeeDept(s.getEmployee().getDept());
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
