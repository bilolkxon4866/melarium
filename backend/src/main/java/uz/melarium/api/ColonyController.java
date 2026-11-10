package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import uz.melarium.api.dto.ColonyRequest;
import uz.melarium.api.dto.ColonyResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.ColonyService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/colonies")
public class ColonyController {
    private final ColonyService colonyService;

    public ColonyController(ColonyService colonyService) { this.colonyService = colonyService; }

    @GetMapping
    public List<ColonyResponse> list(@RequestParam UUID apiaryId, @AuthenticationPrincipal AuthenticatedUser actor) {
        return colonyService.list(apiaryId, actor).stream().map(ColonyResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColonyResponse create(@RequestParam UUID apiaryId, @Valid @RequestBody ColonyRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ColonyResponse.from(colonyService.create(apiaryId, body, actor));
    }

    @PutMapping("/{id}")
    public ColonyResponse update(@PathVariable UUID id, @Valid @RequestBody ColonyRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ColonyResponse.from(colonyService.update(id, body, actor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) { colonyService.deactivate(id, actor); }
}
