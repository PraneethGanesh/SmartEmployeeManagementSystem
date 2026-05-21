package com.example.EmployeeManagementSystem.Controller;

import com.example.EmployeeManagementSystem.DTO.OperationLogDTO;
import com.example.EmployeeManagementSystem.Service.OperationLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operation-logs")
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<OperationLogDTO>> getLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        return ResponseEntity.ok(operationLogService.getLogs(operationType, entityType, entityId));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> exportLogsPdf(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        byte[] pdf = operationLogService.exportPdf(operationType, entityType, entityId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operation-logs.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
