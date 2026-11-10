package uz.melarium.api;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.NotificationPreferencesRequest;
import uz.melarium.api.dto.NotificationPreferencesResponse;
import uz.melarium.api.dto.NotificationResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.NotificationPreferencesService;
import uz.melarium.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationPreferencesService preferencesService;

    public NotificationController(NotificationService notificationService, NotificationPreferencesService preferencesService) {
        this.notificationService = notificationService;
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) { return PageResponse.from(notificationService.list(actor, page, size)); }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return notificationService.markRead(id, actor);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedUser actor) {
        return Map.of("updated", notificationService.markAllRead(actor));
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse preferences(@AuthenticationPrincipal AuthenticatedUser actor) {
        return preferencesService.get(actor);
    }

    @PatchMapping("/preferences")
    public NotificationPreferencesResponse updatePreferences(@Valid @RequestBody NotificationPreferencesRequest body,
                                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        return preferencesService.update(body, actor);
    }
}
