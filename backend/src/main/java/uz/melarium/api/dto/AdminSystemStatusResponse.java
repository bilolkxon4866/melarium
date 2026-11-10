package uz.melarium.api.dto;

import java.util.List;

public record AdminSystemStatusResponse(
        String timezone,
        boolean demoMode,
        String smsProvider,
        boolean smsConfigured,
        boolean otpCodeExposureEnabled,
        boolean investmentTermsConfigured,
        boolean paymentsEnabled,
        List<String> configuredPaymentProviders,
        boolean bootstrapAdminConfigured,
        boolean pushConfigured,
        boolean automaticIotConfigured
) { }
