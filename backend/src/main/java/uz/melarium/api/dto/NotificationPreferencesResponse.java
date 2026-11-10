package uz.melarium.api.dto;

import java.time.Instant;

public record NotificationPreferencesResponse(
        boolean investmentUpdates,
        boolean reportUpdates,
        boolean marketingUpdates,
        boolean pushEnabled,
        Instant updatedAt
) { }
