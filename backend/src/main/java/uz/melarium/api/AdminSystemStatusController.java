package uz.melarium.api;

import java.util.List;

import uz.melarium.api.dto.AdminSystemStatusResponse;
import uz.melarium.payment.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/system")
public class AdminSystemStatusController {
    private final String timezone;
    private final boolean demoMode;
    private final String smsProvider;
    private final boolean smsConfigured;
    private final boolean otpCodeExposureEnabled;
    private final boolean investmentTermsConfigured;
    private final boolean paymentsEnabled;
    private final List<String> configuredPaymentProviders;
    private final boolean bootstrapAdminConfigured;

    public AdminSystemStatusController(
            List<PaymentGateway> gateways,
            @Value("${app.timezone:Asia/Tashkent}") String timezone,
            @Value("${app.demo-mode:true}") boolean demoMode,
            @Value("${app.sms.provider:log}") String smsProvider,
            @Value("${app.sms.api-url:}") String smsUrl,
            @Value("${app.sms.api-token:}") String smsToken,
            @Value("${app.security.expose-otp:false}") boolean otpCodeExposureEnabled,
            @Value("${app.investment.terms-version:}") String termsVersion,
            @Value("${app.payments.enabled:false}") boolean paymentsEnabled,
            @Value("${app.security.bootstrap-admin-phone:}") String bootstrapAdminPhone) {
        this.timezone = timezone;
        this.demoMode = demoMode;
        this.smsProvider = smsProvider == null ? "unknown" : smsProvider;
        this.smsConfigured = !this.smsProvider.equalsIgnoreCase("log")
                && smsUrl != null && !smsUrl.isBlank() && smsToken != null && !smsToken.isBlank();
        this.otpCodeExposureEnabled = otpCodeExposureEnabled;
        this.investmentTermsConfigured = termsVersion != null && !termsVersion.isBlank();
        this.configuredPaymentProviders = gateways.stream().filter(PaymentGateway::isConfigured)
                .map(PaymentGateway::provider).sorted().toList();
        this.paymentsEnabled = paymentsEnabled && investmentTermsConfigured && !configuredPaymentProviders.isEmpty();
        this.bootstrapAdminConfigured = bootstrapAdminPhone != null && !bootstrapAdminPhone.isBlank();
    }

    @GetMapping("/status")
    public AdminSystemStatusResponse status() {
        return new AdminSystemStatusResponse(timezone, demoMode, smsProvider, smsConfigured,
                otpCodeExposureEnabled, investmentTermsConfigured, paymentsEnabled, configuredPaymentProviders,
                bootstrapAdminConfigured, false, false);
    }
}
