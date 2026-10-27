package com.servicehub.dto;

import com.servicehub.model.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Avaliação pública, sem dados pessoais do cliente")
public record ReviewResponse(Long id, Long requestId, Long serviceId, Integer rating,
        String comment, LocalDateTime createdAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getRequest().getId(),
            review.getRequest().getService().getId(), review.getRating().intValue(),
            review.getComment(), review.getCreatedAt());
    }
}
