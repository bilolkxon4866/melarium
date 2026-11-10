package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import uz.melarium.api.dto.CreateInvestmentRequest;
import uz.melarium.api.dto.InvestmentResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.InvestmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('INVESTOR')")
@RequestMapping("/api/v1/investments")
public class InvestmentController {
    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) { this.investmentService = investmentService; }

    @GetMapping("/my")
    public List<InvestmentResponse> myOrders(@AuthenticationPrincipal AuthenticatedUser actor) {
        return investmentService.myOrders(actor);
    }

    @GetMapping("/{id}")
    public InvestmentResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return investmentService.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentResponse create(@Valid @RequestBody CreateInvestmentRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return investmentService.createOrder(body, actor);
    }

    @DeleteMapping("/{id}")
    public InvestmentResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return investmentService.cancel(id, actor);
    }
}
