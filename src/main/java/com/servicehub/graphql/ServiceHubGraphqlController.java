package com.servicehub.graphql;

import com.servicehub.dto.CreateServiceRequest;
import com.servicehub.dto.ProviderResponse;
import com.servicehub.dto.ServiceOfferingResponse;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.service.BookingService;
import com.servicehub.service.ProviderService;
import com.servicehub.service.ServiceOfferingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Alternate transport: REST and GraphQL share the same business rules. */
@Controller
public class ServiceHubGraphqlController {

    private final ProviderService providerService;
    private final ServiceOfferingService serviceOfferingService;
    private final BookingService bookingService;

    public ServiceHubGraphqlController(ProviderService providerService,
                                      ServiceOfferingService serviceOfferingService,
                                      BookingService bookingService) {
        this.providerService = providerService;
        this.serviceOfferingService = serviceOfferingService;
        this.bookingService = bookingService;
    }

    @QueryMapping
    public List<ProviderResponse> providers(@Argument Integer first) {
        return providerService.findAll(PageRequest.of(0, pageSize(first), Sort.by("id"))).content();
    }

    @QueryMapping
    public List<ServiceOfferingResponse> servicesByProvider(@Argument Long providerId,
                                                           @Argument Integer first) {
        if (providerId == null || providerId < 1) {
            throw new IllegalArgumentException("providerId deve ser positivo");
        }
        return serviceOfferingService.findByProvider(providerId, PageRequest.of(0, pageSize(first), Sort.by("id"))).content();
    }

    /** One service lookup for the entire provider page, avoiding one query per provider. */
    @BatchMapping(typeName = "Provider", field = "services")
    public Map<ProviderResponse, List<ServiceOfferingResponse>> services(List<ProviderResponse> providers) {
        Map<Long, List<ServiceOfferingResponse>> servicesByProvider = serviceOfferingService.findByProviders(
                providers.stream().map(ProviderResponse::id).toList());
        Map<ProviderResponse, List<ServiceOfferingResponse>> result = new LinkedHashMap<>();
        providers.forEach(provider -> result.put(provider, servicesByProvider.getOrDefault(provider.id(), List.of())));
        return result;
    }

    @MutationMapping
    public ServiceRequestResponse bookService(@Argument @Valid BookServiceInput input) {
        LocalDateTime scheduledAt;
        try {
            scheduledAt = LocalDateTime.parse(input.scheduledAt());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("scheduledAt deve usar o formato ISO local: 2030-12-20T14:00:00");
        }
        // BookingService authenticates CLIENT and derives its ID from the token.
        return bookingService.create(new CreateServiceRequest(input.serviceId(), scheduledAt, input.notes()));
    }

    private int pageSize(Integer first) {
        int size = first == null ? 20 : first;
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("first deve estar entre 1 e 100");
        }
        return size;
    }

    public record BookServiceInput(
            @NotNull @Positive Long serviceId,
            @NotBlank String scheduledAt,
            @Size(max = 2000) String notes) {
    }
}
