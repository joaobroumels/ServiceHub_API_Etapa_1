package com.servicehub.dto;

import com.servicehub.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Dados de um serviço. O prestador é obtido do usuário autenticado")
public record ServiceOfferingRequest(
    @NotBlank @Size(max = 150) @Schema(example = "Manutenção de computadores") String title,
    @Size(max = 5000) @Schema(example = "Diagnóstico e manutenção preventiva") String description,
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
    @Schema(example = "150.00") BigDecimal price,
    @NotNull @Schema(example = "MANUTENCAO_COMPUTADORES") ServiceCategory category
) {}
