package uz.melarium.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 160) String fullName,
        @Email @Size(max = 255) String email
) { }
