package uz.melarium.api.dto;

import java.util.UUID;

public record PaymentWebhookResponse(UUID paymentId, String status, boolean duplicate, boolean manualReviewRequired) { }
