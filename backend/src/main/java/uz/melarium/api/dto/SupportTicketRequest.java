package uz.melarium.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupportTicketRequest(
        @NotBlank @Pattern(regexp = "GENERAL|COMPLAINT|TECHNICAL") String category,
        @NotBlank @Size(max = 180) String subject,
        @NotBlank @Size(max = 4000) String message
) { }
