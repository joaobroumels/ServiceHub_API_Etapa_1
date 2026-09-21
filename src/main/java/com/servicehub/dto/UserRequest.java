package com.servicehub.dto;

import com.servicehub.model.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastrar ou atualizar um usuário")
public record UserRequest(
        @Schema(description = "Nome completo", example = "Maria Silva")
        @NotBlank(message = "O nome completo é obrigatório")
        @Size(max = 100, message = "O nome completo deve ter no máximo 100 caracteres")
        String fullName,

        @Schema(description = "E-mail único", example = "maria@servicehub.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

        @Schema(description = "Senha com pelo menos 8 caracteres", example = "senha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
        String password,

        @Schema(description = "Telefone opcional", example = "62999999999")
        @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Informe um telefone válido")
        String phone,

        @Schema(description = "Tipo de usuário", example = "CLIENT")
        @NotNull(message = "O tipo de usuário é obrigatório")
        UserType userType
) {
}
