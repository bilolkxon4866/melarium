package uz.melarium.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import uz.melarium.domain.UserAccount;

public record UserProfileResponse(
        UUID id,
        String phone,
        String fullName,
        String email,
        String accountStatus,
        List<String> roles,
        Instant createdAt,
        String profileImageData
) {
    public static UserProfileResponse from(UserAccount user, List<String> roles) {
        return new UserProfileResponse(user.getId(), user.getPhone(), user.getFullName(), user.getEmail(), user.getAccountStatus(), roles, user.getCreatedAt(), user.getProfileImageData());
    }
}
