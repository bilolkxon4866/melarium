package uz.melarium.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED|CLOSED") String status
) { }
