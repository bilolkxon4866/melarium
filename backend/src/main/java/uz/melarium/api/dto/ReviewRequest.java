package uz.melarium.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(@NotBlank @Pattern(regexp = "APPROVED|RETURNED") String status) { }
