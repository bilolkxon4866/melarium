package uz.melarium.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 160) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 350000) @Pattern(regexp = "^$|^data:image/jpeg;base64,[A-Za-z0-9+/]+={0,2}$") String profileImageData
) { }
