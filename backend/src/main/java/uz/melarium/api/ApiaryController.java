package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import uz.melarium.api.dto.ApiaryRequest;
import uz.melarium.api.dto.ApiaryResponse;
import uz.melarium.domain.Apiary;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.ApiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/apiaries")
public class ApiaryController {
    private final ApiaryService apiaryService;

    public ApiaryController(ApiaryService apiaryService) { this.apiaryService = apiaryService; }

    @GetMapping
    public List<ApiaryResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return apiaryService.list(actor).stream().map(ApiaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ApiaryResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiaryResponse.from(apiaryService.get(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiaryResponse create(@Valid @RequestBody ApiaryRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiaryResponse.from(apiaryService.create(body, actor));
    }

    @PutMapping("/{id}")
    public ApiaryResponse update(@PathVariable UUID id, @Valid @RequestBody ApiaryRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiaryResponse.from(apiaryService.update(id, body, actor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) { apiaryService.deactivate(id, actor); }
}
