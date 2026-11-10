package uz.melarium.api;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import uz.melarium.api.dto.AuditLogResponse;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.audit.AuditQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {
    private final AuditQueryService auditQueryService;

    public AdminAuditController(AuditQueryService auditQueryService) { this.auditQueryService = auditQueryService; }

    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @Size(max = 100) String action,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return auditQueryService.list(actorId, action, page, size);
    }
}
