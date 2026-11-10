package uz.melarium.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "OTP 6 ta raqamdan iborat bo‘lishi kerak") String code
) { }
