package com.servicehub.dto;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Serviço e perfil público do prestador")
public record ServiceOfferingResponse(Long id, String title, String description, BigDecimal price,
        ServiceCategory category, ProviderResponse provider, boolean active,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ServiceOfferingResponse from(ServiceOffering service) {
        return new ServiceOfferingResponse(service.getId(), service.getTitle(), service.getDescription(),
            service.getPrice(), service.getCategory(), ProviderResponse.from(service.getProvider()),
            service.isActive(), service.getCreatedAt(), service.getUpdatedAt());
    }
}
