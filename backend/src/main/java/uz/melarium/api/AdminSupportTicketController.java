package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.SupportTicketResponse;
import uz.melarium.api.dto.SupportTicketStatusRequest;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.SupportTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/support/tickets")
public class AdminSupportTicketController {
    private static final List<String> STATUSES = List.of("OPEN", "IN_REVIEW", "RESOLVED", "CLOSED");
    private final SupportTicketService service;
    public AdminSupportTicketController(SupportTicketService service) { this.service = service; }

    @GetMapping
    public PageResponse<SupportTicketResponse> list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yordam so‘rovi holati noto‘g‘ri");
        }
        return PageResponse.from(service.adminList(status, page, size));
    }

    @PatchMapping("/{id}/status")
    public SupportTicketResponse review(@PathVariable UUID id, @Valid @RequestBody SupportTicketStatusRequest body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.review(id, body, actor);
    }
}
