package uz.melarium.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ColonyRequest(
        @Size(max = 40) String code,
        @Min(1) @Max(500) Integer count,
        @Size(max = 24) String strength,
        @Size(max = 4000) String description
) { }
