package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.*;
import com.servicehub.repository.*;
import com.servicehub.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Validated
@Transactional(readOnly = true)
public class BookingService {
    private final ServiceRequestRepository requests;
    private final ServiceOfferingRepository offerings;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final InputSanitizer sanitizer;

    public BookingService(ServiceRequestRepository requests, ServiceOfferingRepository offerings,
            UserRepository users, CurrentUser currentUser, InputSanitizer sanitizer) {
        this.requests = requests; this.offerings = offerings; this.users = users;
        this.currentUser = currentUser; this.sanitizer = sanitizer;
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceRequestResponse create(@Valid CreateServiceRequest input) {
        Long clientId = currentUser.id();
        Long providerId = offerings.findProviderId(input.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + input.serviceId()));
        if (providerId.equals(clientId)) throw new BusinessRuleException("Não é permitido contratar o próprio serviço");

        // Mesma ordem de locks usada por exclusão/edição: usuários antes do serviço.
        // Ordenar os usuários evita ciclos se duas contratações ocorrerem ao mesmo tempo.
        Map<Long, User> locked = new HashMap<>();
        Stream.of(clientId, providerId).sorted().forEach(id -> locked.put(id,
            users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"))));
        User client = locked.get(clientId);
        User provider = locked.get(providerId);
        if (!client.isActive() || client.getUserType() != UserType.CLIENT)
            throw new AccessDeniedException("Somente clientes ativos podem contratar serviços");
        if (!provider.isActive() || provider.getUserType() != UserType.PROVIDER)
            throw new BusinessRuleException("Prestador indisponível para contratação");
        ServiceOffering offering = offerings.findByIdForUpdate(input.serviceId())
            .filter(ServiceOffering::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + input.serviceId()));
        String notes = sanitizer.optionalText(input.notes());
        if (notes != null && notes.length() > 2000) throw new IllegalArgumentException("Observações devem ter no máximo 2000 caracteres");
        ServiceRequest booking = new ServiceRequest();
        booking.setService(offering);
        booking.setClient(client);
        booking.setStatus(RequestStatus.PENDING);
        booking.setTotalPrice(offering.getPrice());
        booking.setScheduledAt(input.scheduledAt());
        booking.setNotes(notes);
        return ServiceRequestResponse.from(requests.saveAndFlush(booking));
    }

    @PreAuthorize("isAuthenticated()")
    public PageResponse<ServiceRequestResponse> findAll(Pageable pageable) {
        return PageResponse.from(requests.findVisible(currentUser.id(), currentUser.isAdmin(),
            Paging.safe(pageable, Set.of("id", "status", "scheduledAt", "totalPrice", "createdAt", "updatedAt")))
            .map(ServiceRequestResponse::from));
    }

    @PreAuthorize("isAuthenticated()")
    public ServiceRequestResponse findById(Long id) {
        ServiceRequest booking = requests.findWithDetailsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));
        requireParticipant(booking);
        return ServiceRequestResponse.from(booking);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceRequestResponse changeStatus(Long id, @Valid StatusUpdateRequest input) {
        ServiceRequest booking = requests.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));
        requireParticipant(booking);
        RequestStatus before = booking.getStatus();
        RequestStatus after = input.status();
        if (!before.canTransitionTo(after))
            throw new BusinessRuleException("Transição de status não permitida: " + before + " -> " + after);
        boolean provider = booking.getService().getProvider().getId().equals(currentUser.id());
        boolean clientCancellation = booking.getClient().getId().equals(currentUser.id())
            && after == RequestStatus.CANCELLED && (before == RequestStatus.PENDING || before == RequestStatus.ACCEPTED);
        if (!currentUser.isAdmin() && !provider && !clientCancellation)
            throw new AccessDeniedException("Somente o prestador pode avançar a execução da ordem");
        booking.setStatus(after);
        return ServiceRequestResponse.from(requests.saveAndFlush(booking));
    }

    private void requireParticipant(ServiceRequest request) {
        Long actorId = currentUser.id();
        if (!currentUser.isAdmin() && !request.getClient().getId().equals(actorId)
                && !request.getService().getProvider().getId().equals(actorId))
            throw new AccessDeniedException("A ordem pertence a outros usuários");
    }
}
