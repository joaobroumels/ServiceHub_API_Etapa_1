package com.servicehub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.servicehub.model.UserType;
import com.servicehub.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Cadastro ou atualização. Cadastro público aceita somente CLIENT ou PROVIDER")
public record UserRequest(
        @NotBlank @Size(min = 2, max = 100)
        @Schema(example = "Maria Silva") String fullName,
        @NotBlank @Email @Size(max = 255)
        @Schema(example = "maria@servicehub.com") String email,
        @NotBlank @StrongPassword
        @Schema(example = "Senha123!", accessMode = Schema.AccessMode.WRITE_ONLY) String password,
        @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Informe um telefone válido")
        @Schema(example = "62999999999") String phone,
        @NotNull @JsonAlias("userType")
        @Schema(example = "CLIENT") UserType role,
        @Size(max = 2000) @Schema(example = "Atendimento em Goiânia") String bio,
        @Size(max = 500) @Pattern(regexp = "^$|^https://[^\\s<>]+$", message = "Use uma URL HTTPS válida")
        @Schema(example = "https://example.com/avatar.png") String avatarUrl
) {
    public UserRequest(String fullName, String email, String password, String phone, UserType role) {
        this(fullName, email, password, phone, role, null, null);
    }
    public UserType userType() { return role; }
}
