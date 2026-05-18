package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.MealOrderDTO;
import com.example.EmployeeManagementSystem.DTO.MealOrderRequest;
import com.example.EmployeeManagementSystem.Entity.*;
import com.example.EmployeeManagementSystem.Enum.OrderStatus;
import com.example.EmployeeManagementSystem.Enum.SubscriptionStatus;
import com.example.EmployeeManagementSystem.Repository.*;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MealOrderService {

    private final MealOrderRepository mealOrderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MenuItemRepository menuItemRepository;
    private final EmployeeRepo employeeRepo;
    private final NotificationService notificationService;

    public MealOrderService(MealOrderRepository mealOrderRepository,
                            SubscriptionRepository subscriptionRepository,
                            MenuItemRepository menuItemRepository,
                            EmployeeRepo employeeRepo,
                            NotificationService notificationService) {
        this.mealOrderRepository = mealOrderRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.menuItemRepository = menuItemRepository;
        this.employeeRepo = employeeRepo;
        this.notificationService = notificationService;
    }

    /**
     * Employee places an order against an active subscription.
     * Validates: subscription belongs to employee, subscription is ACTIVE,
     * all menu items belong to the same restaurant as the subscription.
     */
    @Transactional
    public MealOrderDTO placeOrder(MealOrderRequest request, Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + email));

        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException(
                        "Subscription not found: " + request.getSubscriptionId()));

        // Employee can only order on their own subscription
        if (subscription.getEmployee().getEmployeeId() != employee.getEmployeeId()) {
            throw new AccessDeniedException("This subscription does not belong to you");
        }

        // Subscription must be active
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot place order on a " + subscription.getStatus() + " subscription");
        }

        MealOrder order = new MealOrder();
        order.setSubscription(subscription);
        order.setEmployee(employee);
        order.setMealSlot(subscription.getSlot());
        order.setStatus(OrderStatus.PLACED);

        for (MealOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "Menu item not found: " + itemReq.getMenuItemId()));

            // Menu item must belong to the same restaurant as the subscription
            if (!menuItem.getRestaurant().getId().equals(subscription.getRestaurant().getId())) {
                throw new IllegalArgumentException(
                        "Menu item '" + menuItem.getName() + "' does not belong to this subscription's restaurant");
            }

            // Menu item must match the subscription's meal slot
            if (menuItem.getMealSlot() != subscription.getSlot()) {
                throw new IllegalArgumentException(
                        "Menu item '" + menuItem.getName() + "' is not available for " + subscription.getSlot());
            }

            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException(
                        "Menu item '" + menuItem.getName() + "' is currently unavailable");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMealOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemReq.getQuantity());
            order.getItems().add(orderItem);
        }

        MealOrder saved = mealOrderRepository.save(order);
        return toDTO(saved);
    }

    /**
     * Vendor marks an order as ARRIVED — triggers notification to the employee.
     */
    @Transactional
    public MealOrderDTO markArrived(Long orderId, Authentication authentication) {
        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!order.getSubscription().getRestaurant().getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not manage this order's restaurant");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Order must be CONFIRMED before marking it as ARRIVED. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.ARRIVED);
        order.setDeliveredAt(LocalDateTime.now());
        MealOrder saved = mealOrderRepository.save(order);

        // Notify the employee that their meal has arrived
        String restaurantName = order.getSubscription().getRestaurant().getName();
        String mealSlot = order.getMealSlot().name().toLowerCase();
        notificationService.notify(
                order.getEmployee(),
                "Your " + mealSlot + " from " + restaurantName + " has arrived! Please collect your meal.",
                "ORDER_ARRIVED"
        );

        return toDTO(saved);
    }

    /**
     * Vendor confirms an order (they have placed it with external service).
     */
    @Transactional
    public MealOrderDTO confirmOrder(Long orderId, Authentication authentication) {
        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String vendorEmail = AuthUtil.extractEmail(authentication);
        if (!order.getSubscription().getRestaurant().getVendor().getEmail().equals(vendorEmail)) {
            throw new AccessDeniedException("You do not manage this order's restaurant");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Order must be PLACED before confirming. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        MealOrder saved = mealOrderRepository.save(order);

        // Notify the employee that their order is confirmed
        String restaurantName = order.getSubscription().getRestaurant().getName();
        String mealSlot = order.getMealSlot().name().toLowerCase();
        notificationService.notify(
                order.getEmployee(),
                "Your " + mealSlot + " order from " + restaurantName + " has been confirmed and is on its way.",
                "ORDER_CONFIRMED"
        );

        return toDTO(saved);
    }

    /** Employee views their own orders */
    public List<MealOrderDTO> getMyOrders(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + email));

        return mealOrderRepository
                .findByEmployeeEmployeeIdOrderByPlacedAtDesc(employee.getEmployeeId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Vendor views all orders for their restaurants */
    public List<MealOrderDTO> getVendorOrders(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        // Get vendor by email via their restaurants — find vendor entity
        // Using the restaurant's vendor email match
        return mealOrderRepository.findAll()
                .stream()
                .filter(o -> o.getSubscription().getRestaurant().getVendor().getEmail().equals(email))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Vendor views only PLACED (pending) orders needing action */
    public List<MealOrderDTO> getVendorPendingOrders(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        return mealOrderRepository.findAll()
                .stream()
                .filter(o -> o.getSubscription().getRestaurant().getVendor().getEmail().equals(email)
                        && o.getStatus() == OrderStatus.PLACED)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private MealOrderDTO toDTO(MealOrder order) {
        MealOrderDTO dto = new MealOrderDTO();
        dto.setId(order.getId());
        dto.setSubscriptionId(order.getSubscription().getId());
        dto.setEmployeeId(order.getEmployee().getEmployeeId());
        dto.setEmployeeName(order.getEmployee().getName());
        dto.setMealSlot(order.getMealSlot());
        dto.setStatus(order.getStatus());
        dto.setPlacedAt(order.getPlacedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setRestaurantName(order.getSubscription().getRestaurant().getName());

        List<MealOrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream().map(oi -> {
            MealOrderDTO.OrderItemDTO itemDTO = new MealOrderDTO.OrderItemDTO();
            itemDTO.setMenuItemId(oi.getMenuItem().getId());
            itemDTO.setMenuItemName(oi.getMenuItem().getName());
            itemDTO.setPrice(oi.getMenuItem().getPrice());
            itemDTO.setQuantity(oi.getQuantity());
            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }
}