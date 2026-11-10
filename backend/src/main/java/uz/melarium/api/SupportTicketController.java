package uz.melarium.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.SupportTicketRequest;
import uz.melarium.api.dto.SupportTicketResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.SupportTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasAnyRole('USER', 'INVESTOR', 'BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/support/tickets")
public class SupportTicketController {
    private final SupportTicketService service;
    public SupportTicketController(SupportTicketService service) { this.service = service; }

    @GetMapping
    public PageResponse<SupportTicketResponse> mine(@AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.mine(actor, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse create(@Valid @RequestBody SupportTicketRequest body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.create(body, actor);
    }
}
