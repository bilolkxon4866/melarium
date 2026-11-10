package uz.melarium.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @NotEmpty @Size(max = 4) List<@Pattern(regexp = "INVESTOR|BEEKEEPER|ADMIN|SUPER_ADMIN") String> roles
) { }
