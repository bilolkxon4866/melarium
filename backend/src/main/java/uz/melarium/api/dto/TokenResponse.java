package uz.melarium.api.dto;

import java.util.List;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserView user
) {
    public record UserView(UUID id, String phone, String fullName, List<String> roles) { }
}
