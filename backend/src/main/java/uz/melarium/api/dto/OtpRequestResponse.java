package uz.melarium.api.dto;

public record OtpRequestResponse(
        String phone,
        long expiresInSeconds,
        String deliveryMode,
        String debugCode
) { }
