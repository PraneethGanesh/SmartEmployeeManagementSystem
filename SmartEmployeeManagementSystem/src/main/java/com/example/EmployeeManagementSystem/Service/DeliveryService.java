package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.VendorDTO;
import com.example.EmployeeManagementSystem.DTO.VendorRequest;
import com.example.EmployeeManagementSystem.Entity.Delivery;
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
    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    public DeliveryService(SubscriptionRepository subscriptionRepository,
                           DeliveryRepository deliveryRepository,
                           DeliveryTimeService deliveryTimeService) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryTimeService = deliveryTimeService;
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
                deliveryRepository.save(delivery);

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
    public Delivery updateDeliveryStatus(Long deliveryId, DeliveryStatus status) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        delivery.setStatus(status);
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryTime(Instant.now());
        }

        return deliveryRepository.save(delivery);
    }

    public List<Delivery> getDeliveriesBySubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        return deliveryRepository.findRecentDeliveriesBySubscription(subscription, DeliveryStatus.SCHEDULED);
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
            vendor.setRole(Role.VENDOR);
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
            dto.setRegisteredAt(vendor.getRegisteredAt());
            return dto;
        }
    }
}
