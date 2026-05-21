package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.DeliveryDTO;
import com.example.EmployeeManagementSystem.DTO.VendorDTO;
import com.example.EmployeeManagementSystem.DTO.VendorRequest;
import com.example.EmployeeManagementSystem.Entity.Delivery;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.Restaurant;
import com.example.EmployeeManagementSystem.Entity.Subscription;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Enum.DeliveryStatus;
import com.example.EmployeeManagementSystem.Enum.Role;
import com.example.EmployeeManagementSystem.Enum.SubscriptionStatus;
import com.example.EmployeeManagementSystem.Exception.VendorNotFoundException;
import com.example.EmployeeManagementSystem.Repository.DeliveryRepository;
import com.example.EmployeeManagementSystem.Repository.SubscriptionRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryService {
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryTimeService deliveryTimeService;
    private final OperationLogService operationLogService;
    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    public DeliveryService(SubscriptionRepository subscriptionRepository,
                           DeliveryRepository deliveryRepository,
                           DeliveryTimeService deliveryTimeService,
                           OperationLogService operationLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryTimeService = deliveryTimeService;
        this.operationLogService = operationLogService;
    }

    // AFTER
    @Transactional
    public void processDueDeliveries() {
        Instant now = Instant.now();
        List<Subscription> dueSubscriptions = subscriptionRepository
                .findDueSubscriptions(SubscriptionStatus.ACTIVE.name(), now);

        for (Subscription subscription : dueSubscriptions) {
            try {
                Delivery delivery = new Delivery();
                delivery.setSubscription(subscription);
                delivery.setMealSlot(subscription.getSlot());
                delivery.setScheduledDeliveryTime(subscription.getNextDeliveryTime());
                delivery.setStatus(DeliveryStatus.IN_PROGRESS);
                Delivery savedDelivery = deliveryRepository.save(delivery);
                operationLogService.recordSystem("DELIVERY", "Delivery", savedDelivery.getId(),
                        "SCHEDULE_DELIVERY", null, savedDelivery.getStatus(),
                        "Delivery created for subscription #" + subscription.getId());

                Instant nextDelivery = deliveryTimeService.getNextDeliveryTime(subscription);
                subscription.setNextDeliveryTime(nextDelivery);
                subscriptionRepository.save(subscription);

            } catch (DataIntegrityViolationException e) {
                // Duplicate delivery already exists — just advance the next delivery time
                logger.warn("Duplicate delivery skipped for subscription ID: {}", subscription.getId());
                Instant nextDelivery = deliveryTimeService.getNextDeliveryTime(subscription);
                subscription.setNextDeliveryTime(nextDelivery);
                subscriptionRepository.save(subscription);
            }
        }
    }

    @Transactional
    public DeliveryDTO updateDeliveryStatus(Long deliveryId, DeliveryStatus status, Authentication authentication) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        DeliveryStatus previousStatus = delivery.getStatus();
        delivery.setStatus(status);
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryTime(Instant.now());
        }

        Delivery saved = deliveryRepository.save(delivery);
        operationLogService.record(authentication, "DELIVERY", "Delivery", saved.getId(),
                "UPDATE_DELIVERY_STATUS", previousStatus, saved.getStatus(),
                "Delivery status updated for subscription #" + saved.getSubscription().getId());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesBySubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        return deliveryRepository.findRecentDeliveriesBySubscription(subscription, DeliveryStatus.SCHEDULED)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveries() {
        return deliveryRepository.findAll().stream().map(this::toDTO).toList();
    }

    private DeliveryDTO toDTO(Delivery delivery) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setId(delivery.getId());
        dto.setMealSlot(delivery.getMealSlot());
        dto.setScheduledDeliveryTime(delivery.getScheduledDeliveryTime());
        dto.setActualDeliveryTime(delivery.getActualDeliveryTime());
        dto.setStatus(delivery.getStatus());
        dto.setCreatedAt(delivery.getCreatedAt());

        Subscription subscription = delivery.getSubscription();
        if (subscription != null) {
            dto.setSubscriptionId(subscription.getId());
            Employee employee = subscription.getEmployee();
            if (employee != null) {
                dto.setEmployeeId(employee.getEmployeeId());
                dto.setEmployeeName(employee.getName());
                dto.setEmployeeEmail(employee.getEmail());
                dto.setEmployeeDept(employee.getDept());
            }
            Restaurant restaurant = subscription.getRestaurant();
            if (restaurant != null) {
                dto.setRestaurantId(restaurant.getId());
                dto.setRestaurantName(restaurant.getName());
            }
        }
        return dto;
    }

    @Service
    public static class VendorService {

        private static final Logger log = LoggerFactory.getLogger(VendorService.class);
        private final VendorRepo vendorRepo;
        private final PasswordEncoder passwordEncoder;

        public VendorService(VendorRepo vendorRepo, PasswordEncoder passwordEncoder) {
            this.vendorRepo = vendorRepo;
            this.passwordEncoder = passwordEncoder;
        }

        public VendorDTO registerVendor(VendorRequest request) {
            if (vendorRepo.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException(
                        "A vendor with email " + request.getEmail() + " already exists");
            }
            Vendor vendor = new Vendor();
            vendor.setName(request.getName());
            vendor.setEmail(request.getEmail());
            vendor.setPhone(request.getPhone());
            vendor.setRole(request.getRole());
            vendor.setPassword(passwordEncoder.encode(request.getPassword()));
            Vendor saved = vendorRepo.save(vendor);
            log.info("Vendor registered: {} (id={})", saved.getName(), saved.getId());
            return toDTO(saved);
        }

        public VendorDTO getVendor(Authentication authentication) {
            String email= AuthUtil.extractEmail(authentication);
            return toDTO(vendorRepo.findByEmail(email).orElseThrow(
                    ()->new VendorNotFoundException("Cannot get others accoun")
            ));
        }

        public List<VendorDTO> getAllVendors() {
            return vendorRepo.findAll().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }

        public VendorDTO updateVendor(VendorRequest request,Authentication authentication) {
            String email= AuthUtil.extractEmail(authentication);
            Vendor vendor = vendorRepo.findByEmail(email).orElseThrow(
                    ()->new VendorNotFoundException("Cannot update others account")
            );
            if (request.getName() != null) vendor.setName(request.getName());
            if (request.getPhone() != null) vendor.setPhone(request.getPhone());
            if (request.getPassword() != null) vendor.setPassword(passwordEncoder.encode(request.getPassword()));
            // Email update not allowed to avoid breaking restaurant ownership references
            return toDTO(vendorRepo.save(vendor));
        }

        public void deleteVendor(Authentication authentication) {
            String email= AuthUtil.extractEmail(authentication);
            Vendor vendor = vendorRepo.findByEmail(email).orElseThrow(
                    ()->new VendorNotFoundException("Cannot delete others account")
            );
            vendorRepo.delete(vendor);
            log.info("Vendor deleted: email={}", email);
        }

        public void deleteVendorById(Long vendorId) {
            Vendor vendor = findOrThrow(vendorId);
            vendorRepo.delete(vendor);
            log.info("Vendor deleted by admin: id={}, email={}", vendor.getId(), vendor.getEmail());
        }

        // ── helpers ──────────────────────────────────────────────────────────────

        private Vendor findOrThrow(Long id) {
            return vendorRepo.findById(id)
                    .orElseThrow(() -> new VendorNotFoundException("Vendor with id " + id + " not found"));
        }

        private VendorDTO toDTO(Vendor vendor) {
            VendorDTO dto = new VendorDTO();
            dto.setId(vendor.getId());
            dto.setName(vendor.getName());
            dto.setEmail(vendor.getEmail());
            dto.setPhone(vendor.getPhone());
            dto.setRole(vendor.getRole());
            dto.setRegisteredAt(vendor.getRegisteredAt());
            return dto;
        }
    }
}
