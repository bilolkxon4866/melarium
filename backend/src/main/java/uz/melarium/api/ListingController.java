package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.ListingCreateRequest;
import uz.melarium.api.dto.ListingResponse;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.ListingStatusRequest;
import uz.melarium.api.dto.ListingUpdateRequest;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.ListingService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/listings")
public class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) { this.listingService = listingService; }

    @GetMapping
    public PageResponse<ListingResponse> listActive(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return PageResponse.from(listingService.active(query, page, size));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
    public List<ListingResponse> myListings(@AuthenticationPrincipal AuthenticatedUser actor) {
        return listingService.myListings(actor);
    }

    @GetMapping("/moderation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public PageResponse<ListingResponse> moderationQueue(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return PageResponse.from(listingService.moderationQueue(page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(@Valid @RequestBody ListingCreateRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return listingService.create(body, actor);
    }

    @GetMapping("/{id}")
    public ListingResponse getActive(@PathVariable UUID id) { return listingService.activeById(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
    public ListingResponse update(@PathVariable UUID id, @Valid @RequestBody ListingUpdateRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return listingService.update(id, body, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ListingResponse moderate(@PathVariable UUID id, @Valid @RequestBody ListingStatusRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return listingService.moderate(id, body, actor);
    }
}
