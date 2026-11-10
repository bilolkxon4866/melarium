package uz.melarium.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiaryRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 240) String location,
        @Size(max = 4000) String description
) { }
