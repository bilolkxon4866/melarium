package uz.melarium.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupportTicketStatusRequest(
        @NotBlank @Pattern(regexp = "OPEN|IN_REVIEW|RESOLVED|CLOSED") String status,
        @Size(max = 2000) String adminNote
) { }
