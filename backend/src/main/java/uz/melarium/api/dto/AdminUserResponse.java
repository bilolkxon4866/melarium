package uz.melarium.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import uz.melarium.domain.UserAccount;

public record AdminUserResponse(
        UUID id,
        String phone,
        String fullName,
        String email,
        String accountStatus,
        List<String> roles,
        Instant createdAt
) {
    public static AdminUserResponse from(UserAccount user, List<String> roles) {
        return new AdminUserResponse(user.getId(), user.getPhone(), user.getFullName(), user.getEmail(), user.getAccountStatus(), roles, user.getCreatedAt());
    }
}
