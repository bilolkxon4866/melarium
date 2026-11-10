package uz.melarium.api;

import java.util.List;

import jakarta.validation.Valid;
import uz.melarium.api.dto.ExpenseRequest;
import uz.melarium.api.dto.ExpenseResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;
    public ExpenseController(ExpenseService expenseService) { this.expenseService = expenseService; }

    @GetMapping
    public List<ExpenseResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return expenseService.list(actor).stream().map(ExpenseResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest body, @AuthenticationPrincipal AuthenticatedUser actor) {
        return ExpenseResponse.from(expenseService.create(body, actor));
    }
}
