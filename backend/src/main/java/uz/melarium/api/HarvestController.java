package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import uz.melarium.api.dto.HarvestRequest;
import uz.melarium.api.dto.HarvestResponse;
import uz.melarium.api.dto.ReviewRequest;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.HarvestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/harvests")
public class HarvestController {
    private final HarvestService harvestService;
    public HarvestController(HarvestService harvestService) { this.harvestService = harvestService; }

    @GetMapping
    public List<HarvestResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return harvestService.list(actor).stream().map(HarvestResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HarvestResponse create(@Valid @RequestBody HarvestRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return HarvestResponse.from(harvestService.create(body, actor));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public HarvestResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return HarvestResponse.from(harvestService.review(id, body.status(), actor));
    }
}
