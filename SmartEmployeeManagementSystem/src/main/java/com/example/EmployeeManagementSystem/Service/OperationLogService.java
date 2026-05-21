package com.example.EmployeeManagementSystem.Service;

import com.example.EmployeeManagementSystem.DTO.OperationLogDTO;
import com.example.EmployeeManagementSystem.Entity.Employee;
import com.example.EmployeeManagementSystem.Entity.OperationLog;
import com.example.EmployeeManagementSystem.Entity.Vendor;
import com.example.EmployeeManagementSystem.Repository.EmployeeRepo;
import com.example.EmployeeManagementSystem.Repository.OperationLogRepository;
import com.example.EmployeeManagementSystem.Repository.VendorRepo;
import com.example.EmployeeManagementSystem.Util.AuthUtil;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OperationLogService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogRepository operationLogRepository;
    private final EmployeeRepo employeeRepo;
    private final VendorRepo vendorRepo;

    public OperationLogService(OperationLogRepository operationLogRepository,
                               EmployeeRepo employeeRepo,
                               VendorRepo vendorRepo) {
        this.operationLogRepository = operationLogRepository;
        this.employeeRepo = employeeRepo;
        this.vendorRepo = vendorRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperationLog record(Authentication authentication,
                               String operationType,
                               String entityType,
                               Long entityId,
                               String action,
                               Object previousStatus,
                               Object newStatus,
                               String details) {
        String email = AuthUtil.extractEmail(authentication);
        Actor actor = resolveActor(email, authentication);
        return save(operationType, entityType, entityId, action, previousStatus, newStatus,
                actor.name(), actor.email(), actor.role(), details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperationLog recordSystem(String operationType,
                                     String entityType,
                                     Long entityId,
                                     String action,
                                     Object previousStatus,
                                     Object newStatus,
                                     String details) {
        return save(operationType, entityType, entityId, action, previousStatus, newStatus,
                "System", "system", "SYSTEM", details);
    }

    @Transactional(readOnly = true)
    public List<OperationLogDTO> getLogs(String operationType, String entityType, Long entityId) {
        List<OperationLog> logs;
        if (entityType != null && entityId != null) {
            logs = operationLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId);
        } else if (operationType != null && !operationType.isBlank()) {
            logs = operationLogRepository.findByOperationTypeOrderByOccurredAtDesc(operationType);
        } else {
            logs = operationLogRepository.findAllByOrderByOccurredAtDesc();
        }
        return logs.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(String operationType, String entityType, Long entityId) {
        List<OperationLogDTO> logs = getLogs(operationType, entityType, entityId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);

        try (Document document = new Document(pdf)) {
            document.add(new Paragraph("Operation Audit Logs").setBold().setFontSize(16));
            document.add(new Paragraph("Generated at: " + LocalDateTime.now().format(DATE_TIME)).setFontSize(9));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1.2f, 1.2f, 1.2f, 1.5f, 1.5f, 1.6f, 2.2f}));
            table.setWidth(UnitValue.createPercentValue(100));
            addHeader(table, "Time");
            addHeader(table, "Operation");
            addHeader(table, "Entity");
            addHeader(table, "Action");
            addHeader(table, "Status");
            addHeader(table, "Performer");
            addHeader(table, "Details");

            for (OperationLogDTO log : logs) {
                table.addCell(cell(format(log.getOccurredAt())));
                table.addCell(cell(log.getOperationType()));
                table.addCell(cell(log.getEntityType() + " #" + log.getEntityId()));
                table.addCell(cell(log.getAction()));
                table.addCell(cell(statusText(log)));
                table.addCell(cell(log.getPerformedByName() + "\n" + nullSafe(log.getPerformedByRole())));
                table.addCell(cell(log.getDetails()));
            }
            document.add(table);
        }
        return out.toByteArray();
    }

    private OperationLog save(String operationType,
                              String entityType,
                              Long entityId,
                              String action,
                              Object previousStatus,
                              Object newStatus,
                              String performedByName,
                              String performedByEmail,
                              String performedByRole,
                              String details) {
        OperationLog log = new OperationLog();
        log.setOperationType(operationType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPreviousStatus(status(previousStatus));
        log.setNewStatus(status(newStatus));
        log.setPerformedByName(performedByName);
        log.setPerformedByEmail(performedByEmail);
        log.setPerformedByRole(performedByRole);
        log.setDetails(details);
        return operationLogRepository.save(log);
    }

    private Actor resolveActor(String email, Authentication authentication) {
        if (email == null || email.isBlank()) {
            return new Actor("Unknown", null, role(authentication));
        }
        return employeeRepo.findByEmail(email)
                .map(employee -> new Actor(displayEmployee(employee), employee.getEmail(), role(authentication)))
                .or(() -> vendorRepo.findByEmail(email)
                        .map(vendor -> new Actor(displayVendor(vendor), vendor.getEmail(), role(authentication))))
                .orElse(new Actor(email, email, role(authentication)));
    }

    private OperationLogDTO toDTO(OperationLog log) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(log.getId());
        dto.setOperationType(log.getOperationType());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setAction(log.getAction());
        dto.setPreviousStatus(log.getPreviousStatus());
        dto.setNewStatus(log.getNewStatus());
        dto.setPerformedByName(log.getPerformedByName());
        dto.setPerformedByEmail(log.getPerformedByEmail());
        dto.setPerformedByRole(log.getPerformedByRole());
        dto.setDetails(log.getDetails());
        dto.setOccurredAt(log.getOccurredAt());
        return dto;
    }

    private static void addHeader(Table table, String value) {
        table.addHeaderCell(new Cell().add(new Paragraph(value).setBold().setFontSize(8)));
    }

    private static Cell cell(String value) {
        return new Cell().add(new Paragraph(nullSafe(value)).setFontSize(7));
    }

    private static String status(Object value) {
        return value == null ? null : value.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private static String statusText(OperationLogDTO log) {
        if (log.getPreviousStatus() == null && log.getNewStatus() == null) {
            return "";
        }
        return nullSafe(log.getPreviousStatus()) + " -> " + nullSafe(log.getNewStatus());
    }

    private static String role(Authentication authentication) {
        if (authentication == null) {
            return "UNKNOWN";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .map(authority -> authority.substring("ROLE_".length()))
                .orElse("UNKNOWN");
    }

    private static String displayEmployee(Employee employee) {
        return employee.getName() != null ? employee.getName() : employee.getEmail();
    }

    private static String displayVendor(Vendor vendor) {
        return vendor.getName() != null ? vendor.getName() : vendor.getEmail();
    }

    private record Actor(String name, String email, String role) {}
}
