package uz.melarium.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Email @Size(max = 255) String email
) { }
