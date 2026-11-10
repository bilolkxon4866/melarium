package uz.melarium.api;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.SensorReadingRequest;
import uz.melarium.api.dto.SensorReadingResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.SensorReadingService;
import org.springframework.data.domain.Page;
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
@PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/sensor-readings")
public class SensorReadingController {
    private final SensorReadingService sensorService;
    public SensorReadingController(SensorReadingService sensorService) { this.sensorService = sensorService; }

    @GetMapping
    public PageResponse<SensorReadingResponse> list(
            @RequestParam UUID apiaryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return PageResponse.from(sensorService.list(apiaryId, page, size, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingResponse create(@Valid @RequestBody SensorReadingRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return sensorService.create(body, actor);
    }
}
