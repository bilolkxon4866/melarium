package uz.melarium.api.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequest(
        @NotNull Boolean investmentUpdates,
        @NotNull Boolean reportUpdates,
        @NotNull Boolean marketingUpdates,
        @NotNull Boolean pushEnabled
) { }
