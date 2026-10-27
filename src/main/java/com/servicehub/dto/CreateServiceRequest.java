package com.servicehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Schema(description = "Contratação. Cliente, preço e status são determinados pelo servidor")
public record CreateServiceRequest(
    @NotNull @Positive @Schema(example = "1") Long serviceId,
    @Future @Schema(example = "2027-12-15T14:00:00") LocalDateTime scheduledAt,
    @Size(max = 2000) @Schema(example = "Preferência pelo período da tarde") String notes
) {}
