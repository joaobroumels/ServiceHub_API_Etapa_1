package com.servicehub.dto;

import com.servicehub.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil público do prestador; não expõe contatos, senha ou credenciais")
public record ProviderResponse(Long id, String fullName, String bio, String avatarUrl) {
    public static ProviderResponse from(User user) {
        return new ProviderResponse(user.getId(), user.getFullName(), user.getBio(), user.getAvatarUrl());
    }
}
