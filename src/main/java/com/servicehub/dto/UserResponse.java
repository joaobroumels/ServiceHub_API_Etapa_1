package com.servicehub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.servicehub.model.User;
import com.servicehub.model.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Perfil privado do usuário. Nunca contém senha ou hash")
public record UserResponse(Long id, String fullName, String email, String phone,
                           UserType role, String bio, String avatarUrl, boolean active,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getUserType(), user.getBio(), user.getAvatarUrl(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
    @JsonProperty("userType")
    @Schema(description = "Alias legado de role")
    public UserType userType() { return role; }
}
