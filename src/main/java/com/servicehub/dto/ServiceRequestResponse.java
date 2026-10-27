package com.servicehub.dto;

import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ordem de serviço, acessível apenas às partes envolvidas e administradores")
public record ServiceRequestResponse(Long id, Long serviceId, String serviceTitle, Long providerId,
        Long clientId, RequestStatus status, LocalDateTime scheduledAt, String notes,
        BigDecimal totalPrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ServiceRequestResponse from(ServiceRequest request) {
        return new ServiceRequestResponse(request.getId(), request.getService().getId(),
            request.getService().getTitle(), request.getService().getProvider().getId(),
            request.getClient().getId(), request.getStatus(), request.getScheduledAt(),
            request.getNotes(), request.getTotalPrice(), request.getCreatedAt(), request.getUpdatedAt());
    }
}
