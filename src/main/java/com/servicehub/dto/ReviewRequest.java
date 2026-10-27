package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Avaliação única pelo cliente da ordem concluída")
public record ReviewRequest(
    @NotNull @Positive @Schema(example = "1") Long requestId,
    @NotNull @Min(1) @Max(5) @Schema(example = "5") Integer rating,
    @Size(max = 2000) @Schema(example = "Serviço realizado com qualidade") String comment
) {}
