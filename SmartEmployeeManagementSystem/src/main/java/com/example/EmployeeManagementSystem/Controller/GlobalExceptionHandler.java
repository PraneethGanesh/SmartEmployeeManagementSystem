package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.Exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> buildError(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

    @ExceptionHandler(EmployeeNotFound.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFound(
            EmployeeNotFound ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidStartDateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStartDate(
            InvalidStartDateException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidEndDateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEndDate(
            InvalidEndDateException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OverlappingLeaveException.class)
    public ResponseEntity<Map<String, Object>> handleOverlappingLeave(
            OverlappingLeaveException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateRequest(
            DuplicateRequestException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LeaveRequestNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLeaveNotFound(
            LeaveRequestNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidManagerException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidManager(
            InvalidManagerException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SubscriptionAlreadyExists.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionExists(
            SubscriptionAlreadyExists ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.CONFLICT, ex.getMessage(), request),
                HttpStatus.CONFLICT);
    }


    @ExceptionHandler(VendorNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVendorNotFound(
            VendorNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRestaurantNotFound(
            RestaurantNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedAccessException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        return new ResponseEntity<>(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex, WebRequest request) {
        return new ResponseEntity<>(
                buildError(HttpStatus.CONFLICT, "Cannot delete: related records exist", request),
                HttpStatus.CONFLICT);
    }
}