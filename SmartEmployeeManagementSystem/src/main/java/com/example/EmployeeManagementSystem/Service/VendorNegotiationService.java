package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.*;
import com.example.EmployeeManagementSystem.Entity.Device;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Entity.VendorNegotiation;
import com.example.EmployeeManagementSystem.Entity.VendorNegotiationMessage;
import com.example.EmployeeManagementSystem.Enum.MessageSenderType;
import com.example.EmployeeManagementSystem.Enum.NegotiationStatus;
import com.example.EmployeeManagementSystem.Repository.DeviceRepository;
import com.example.EmployeeManagementSystem.Repository.VendorNegotiationMessageRepository;
import com.example.EmployeeManagementSystem.Repository.VendorNegotiationRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VendorNegotiationService {

    private final VendorNegotiationRepository negotiationRepository;
    private final VendorNegotiationMessageRepository messageRepository;
    private final VendorRepo vendorRepository;
    private final DeviceRepository deviceRepository;

    public VendorNegotiationService(VendorNegotiationRepository negotiationRepository,
                                    VendorNegotiationMessageRepository messageRepository,
                                    VendorRepo vendorRepository,
                                    DeviceRepository deviceRepository) {
        this.negotiationRepository = negotiationRepository;
        this.messageRepository = messageRepository;
        this.vendorRepository = vendorRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public VendorNegotiationResponse createNegotiation(VendorNegotiationRequest request,
                                                       Authentication authentication) {
        validateCreateRequest(request);

        MessageSenderType senderType = senderType(authentication);
        Vendor vendor = resolveVendor(request, authentication, senderType);
        Device device = resolveDevice(request.getDeviceId(), vendor);

        VendorNegotiation negotiation = new VendorNegotiation();
        negotiation.setVendor(vendor);
        negotiation.setDevice(device);
        negotiation.setSubject(request.getSubject().trim());
        negotiation.setDescription(request.getDescription().trim());
        negotiation.setProposedAmount(request.getProposedAmount());
        negotiation.setStatus(NegotiationStatus.PENDING);

        VendorNegotiation saved = negotiationRepository.save(negotiation);

        if (hasText(request.getInitialMessage())) {
            saveMessage(saved, senderType, AuthUtil.extractEmail(authentication), request.getInitialMessage());
            saved.setStatus(NegotiationStatus.NEGOTIATING);
            saved = negotiationRepository.save(saved);
        }

        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<VendorNegotiationResponse> listNegotiations(Authentication authentication) {
        if (isAdmin(authentication)) {
            return negotiationRepository.findAllByOrderByUpdatedAtDesc()
                    .stream()
                    .map(n -> toResponse(n, false))
                    .toList();
        }

        Vendor vendor = vendorForAuthentication(authentication);
        return negotiationRepository.findByVendorIdOrderByUpdatedAtDesc(vendor.getId())
                .stream()
                .map(n -> toResponse(n, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorNegotiationResponse getNegotiation(Long negotiationId, Authentication authentication) {
        VendorNegotiation negotiation = findNegotiation(negotiationId);
        assertParticipantAccess(negotiation, authentication);
        return toResponse(negotiation, true);
    }

    @Transactional
    public VendorNegotiationResponse addMessage(Long negotiationId,
                                                VendorNegotiationMessageRequest request,
                                                Authentication authentication) {
        if (request == null || !hasText(request.getMessage())) {
            throw new IllegalArgumentException("Message is required");
        }

        VendorNegotiation negotiation = findNegotiation(negotiationId);
        assertParticipantAccess(negotiation, authentication);

        saveMessage(negotiation, senderType(authentication), AuthUtil.extractEmail(authentication), request.getMessage());
        if (negotiation.getStatus() == NegotiationStatus.PENDING) {
            negotiation.setStatus(NegotiationStatus.NEGOTIATING);
        }
        VendorNegotiation saved = negotiationRepository.save(negotiation);
        return toResponse(saved, true);
    }

    @Transactional
    public VendorNegotiationResponse updateStatus(Long negotiationId,
                                                  VendorNegotiationStatusRequest request,
                                                  Authentication authentication) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        VendorNegotiation negotiation = findNegotiation(negotiationId);
        assertParticipantAccess(negotiation, authentication);

        if (!isAdmin(authentication) && request.getStatus() == NegotiationStatus.PAID) {
            throw new AccessDeniedException("Only admin can mark a negotiation as paid");
        }

        negotiation.setStatus(request.getStatus());
        return toResponse(negotiationRepository.save(negotiation), true);
    }

    private void validateCreateRequest(VendorNegotiationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Negotiation request is required");
        }
        if (!hasText(request.getSubject())) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (!hasText(request.getDescription())) {
            throw new IllegalArgumentException("Description is required");
        }
    }

    private Vendor resolveVendor(VendorNegotiationRequest request,
                                 Authentication authentication,
                                 MessageSenderType senderType) {
        if (senderType == MessageSenderType.ADMIN) {
            if (request.getVendorId() == null) {
                throw new IllegalArgumentException("vendorId is required when admin creates a negotiation");
            }
            return vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + request.getVendorId()));
        }

        return vendorForAuthentication(authentication);
    }

    private Device resolveDevice(Long deviceId, Vendor vendor) {
        if (deviceId == null) {
            return null;
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        if (device.getTechVendor() != null && !device.getTechVendor().getId().equals(vendor.getId())) {
            throw new AccessDeniedException("Device does not belong to this vendor");
        }

        return device;
    }

    private Vendor vendorForAuthentication(Authentication authentication) {
        String email = AuthUtil.extractEmail(authentication);
        return vendorRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Vendor account not found for " + email));
    }

    private VendorNegotiation findNegotiation(Long negotiationId) {
        return negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new IllegalArgumentException("Negotiation not found: " + negotiationId));
    }

    private void assertParticipantAccess(VendorNegotiation negotiation, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }

        Vendor vendor = vendorForAuthentication(authentication);
        if (!negotiation.getVendor().getId().equals(vendor.getId())) {
            throw new AccessDeniedException("You cannot access another vendor's negotiation");
        }
    }

    private void saveMessage(VendorNegotiation negotiation,
                             MessageSenderType senderType,
                             String senderEmail,
                             String text) {
        VendorNegotiationMessage message = new VendorNegotiationMessage();
        message.setNegotiation(negotiation);
        message.setSenderType(senderType);
        message.setSenderEmail(senderEmail);
        message.setMessage(text.trim());
        messageRepository.save(message);
    }

    private VendorNegotiationResponse toResponse(VendorNegotiation negotiation, boolean includeMessages) {
        VendorNegotiationResponse response = new VendorNegotiationResponse();
        response.setId(negotiation.getId());
        response.setVendorId(negotiation.getVendor().getId());
        response.setVendorName(negotiation.getVendor().getName());
        response.setSubject(negotiation.getSubject());
        response.setDescription(negotiation.getDescription());
        response.setProposedAmount(negotiation.getProposedAmount());
        response.setStatus(negotiation.getStatus());
        response.setCreatedAt(negotiation.getCreatedAt());
        response.setUpdatedAt(negotiation.getUpdatedAt());

        if (negotiation.getDevice() != null) {
            response.setDeviceId(negotiation.getDevice().getId());
            response.setDeviceName(negotiation.getDevice().getDeviceName());
        }

        if (includeMessages) {
            response.setMessages(messageRepository.findByNegotiationIdOrderByCreatedAtAsc(negotiation.getId())
                    .stream()
                    .map(this::toMessageResponse)
                    .toList());
        }

        return response;
    }

    private VendorNegotiationMessageResponse toMessageResponse(VendorNegotiationMessage message) {
        VendorNegotiationMessageResponse response = new VendorNegotiationMessageResponse();
        response.setId(message.getId());
        response.setSenderType(message.getSenderType());
        response.setSenderEmail(message.getSenderEmail());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private MessageSenderType senderType(Authentication authentication) {
        return isAdmin(authentication) ? MessageSenderType.ADMIN : MessageSenderType.VENDOR;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
