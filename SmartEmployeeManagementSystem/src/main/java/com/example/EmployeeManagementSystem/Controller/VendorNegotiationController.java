package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.VendorNegotiationMessageRequest;
import com.example.EmployeeManagementSystem.DTO.VendorNegotiationRequest;
import com.example.EmployeeManagementSystem.DTO.VendorNegotiationResponse;
import com.example.EmployeeManagementSystem.DTO.VendorNegotiationStatusRequest;
import com.example.EmployeeManagementSystem.Service.VendorNegotiationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor-negotiations")
@PreAuthorize("hasAnyRole('ADMIN','TECH_VENDOR','FOOD_VENDOR')")
public class VendorNegotiationController {

    private final VendorNegotiationService negotiationService;

    public VendorNegotiationController(VendorNegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @PostMapping
    public ResponseEntity<VendorNegotiationResponse> createNegotiation(
            @RequestBody VendorNegotiationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negotiationService.createNegotiation(request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<VendorNegotiationResponse>> listNegotiations(Authentication authentication) {
        return ResponseEntity.ok(negotiationService.listNegotiations(authentication));
    }

    @GetMapping("/{negotiationId}")
    public ResponseEntity<VendorNegotiationResponse> getNegotiation(
            @PathVariable Long negotiationId,
            Authentication authentication) {
        return ResponseEntity.ok(negotiationService.getNegotiation(negotiationId, authentication));
    }

    @PostMapping("/{negotiationId}/messages")
    public ResponseEntity<VendorNegotiationResponse> addMessage(
            @PathVariable Long negotiationId,
            @RequestBody VendorNegotiationMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negotiationService.addMessage(negotiationId, request, authentication));
    }

    @PutMapping("/{negotiationId}/status")
    public ResponseEntity<VendorNegotiationResponse> updateStatus(
            @PathVariable Long negotiationId,
            @RequestBody VendorNegotiationStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(negotiationService.updateStatus(negotiationId, request, authentication));
    }
}
