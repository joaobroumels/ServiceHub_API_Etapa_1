package com.servicehub.dto;

import com.servicehub.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Próximo status; transições e permissões são verificadas pelo servidor")
public record StatusUpdateRequest(@NotNull @Schema(example = "ACCEPTED") RequestStatus status) {}
