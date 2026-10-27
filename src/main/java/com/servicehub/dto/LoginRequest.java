package com.servicehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Email @Size(max = 255) @JsonAlias("username")
        @Schema(example = "maria@servicehub.com") String email,
        @NotBlank @Size(max = 100) @Schema(example = "Senha123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password) { }
