package uz.melarium.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.InvestmentResponse;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.service.InvestmentService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/investments")
public class AdminInvestmentController {
    private final InvestmentService investmentService;

    public AdminInvestmentController(InvestmentService investmentService) { this.investmentService = investmentService; }

    @GetMapping
    public PageResponse<InvestmentResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        Page<InvestmentResponse> result = investmentService.adminOrders(page, size);
        return PageResponse.from(result);
    }
}
