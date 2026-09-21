package com.servicehub.dto;

import com.servicehub.model.User;
import com.servicehub.model.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados públicos do usuário")
public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Maria Silva") String fullName,
        @Schema(example = "maria@servicehub.com") String email,
        @Schema(example = "62999999999") String phone,
        @Schema(example = "CLIENT") UserType userType,
        @Schema(example = "true") boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getUserType(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
