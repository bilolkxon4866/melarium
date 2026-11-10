package uz.melarium.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.PaymentInitiateRequest;
import uz.melarium.api.dto.PaymentInitiationResponse;
import uz.melarium.api.dto.PaymentResponse;
import uz.melarium.api.dto.PaymentWebhookResponse;
import uz.melarium.payment.PaymentService;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('INVESTOR')")
    public PaymentInitiationResponse initiate(@Valid @RequestBody PaymentInitiateRequest body,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        return paymentService.initiate(body, actor);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INVESTOR')")
    public PageResponse<PaymentResponse> myPayments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        Page<PaymentResponse> result = paymentService.myPayments(page, size, actor);
        return PageResponse.from(result);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public PageResponse<PaymentResponse> adminPayments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return PageResponse.from(paymentService.adminPayments(page, size));
    }

    @PostMapping("/webhook/{provider}")
    @ResponseStatus(HttpStatus.OK)
    public PaymentWebhookResponse webhook(@PathVariable String provider,
                                          @RequestHeader(value = "X-Melarium-Signature", required = false) String signature,
                                          @RequestBody String rawBody) {
        return paymentService.acceptWebhook(provider, signature, rawBody);
    }
}
