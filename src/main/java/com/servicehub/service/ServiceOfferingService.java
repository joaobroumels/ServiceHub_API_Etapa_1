package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.*;
import com.servicehub.repository.*;
import com.servicehub.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
@Transactional(readOnly = true)
public class ServiceOfferingService {
    private final ServiceOfferingRepository offerings;
    private final ServiceRequestRepository requests;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final InputSanitizer sanitizer;

    public ServiceOfferingService(ServiceOfferingRepository offerings, ServiceRequestRepository requests,
            UserRepository users, CurrentUser currentUser, InputSanitizer sanitizer) {
        this.offerings = offerings; this.requests = requests; this.users = users;
        this.currentUser = currentUser; this.sanitizer = sanitizer;
    }

    public PageResponse<ServiceOfferingResponse> findAll(ServiceCategory category, String search, Pageable pageable) {
        pageable = safePage(pageable);
        String term = sanitizer.optionalText(search);
        if (term != null && term.length() > 150) throw new IllegalArgumentException("Busca deve ter no máximo 150 caracteres");
        Specification<ServiceOffering> spec = visible();
        if (category != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        if (term != null) {
            String escaped = term.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
        }
        return PageResponse.from(offerings.findAll(spec, pageable).map(ServiceOfferingResponse::from));
    }

    public ServiceOfferingResponse findById(Long id) { return ServiceOfferingResponse.from(visibleEntity(id)); }

    public PageResponse<ServiceOfferingResponse> findByProvider(Long providerId, Pageable pageable) {
        pageable = safePage(pageable);
        Specification<ServiceOffering> spec = visible().and((root, query, cb) -> cb.equal(root.get("provider").get("id"), providerId));
        return PageResponse.from(offerings.findAll(spec, pageable).map(ServiceOfferingResponse::from));
    }

    /** Batch GraphQL: uma consulta escalar com janela, no máximo 20 ofertas por prestador. */
    public Map<Long, List<ServiceOfferingResponse>> findByProviders(Collection<Long> providerIds) {
        if (providerIds == null || providerIds.size() > 100) throw new IllegalArgumentException("Informe até 100 prestadores");
        Map<Long, List<ServiceOfferingResponse>> grouped = new LinkedHashMap<>();
        providerIds.forEach(id -> grouped.put(id, new ArrayList<>()));
        if (!providerIds.isEmpty()) {
            for (ServiceOfferingRepository.PublicCatalogRow row : offerings.findActiveByProviderIds(providerIds)) {
                grouped.get(row.getProviderId()).add(new ServiceOfferingResponse(row.getId(), row.getTitle(),
                    row.getDescription(), row.getPrice(), ServiceCategory.valueOf(row.getCategory()),
                    new ProviderResponse(row.getProviderId(), row.getProviderName(), row.getProviderBio(), row.getProviderAvatar()),
                    true, row.getCreatedAt(), row.getUpdatedAt()));
            }
        }
        grouped.replaceAll((id, list) -> List.copyOf(list));
        return grouped;
    }

    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public ServiceOfferingResponse create(@Valid ServiceOfferingRequest request) {
        User provider = users.findByIdForUpdate(currentUser.id())
            .filter(User::isActive).filter(u -> u.getUserType() == UserType.PROVIDER)
            .orElseThrow(() -> new ResourceNotFoundException("Prestador ativo não encontrado"));
        ServiceOffering offering = new ServiceOffering();
        offering.setProvider(provider);
        copy(request, offering);
        return ServiceOfferingResponse.from(offerings.saveAndFlush(offering));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ServiceOfferingResponse update(Long id, @Valid ServiceOfferingRequest request) {
        ServiceOffering offering = lockOwned(id);
        copy(request, offering);
        return ServiceOfferingResponse.from(offerings.saveAndFlush(offering));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public void delete(Long id) {
        ServiceOffering offering = lockOwned(id);
        if (requests.existsByServiceIdAndStatusIn(id, RequestStatus.ACTIVE))
            throw new BusinessRuleException("Serviço possui ordens ativas e não pode ser excluído");
        offering.setActive(false);
        offerings.save(offering);
    }

    private ServiceOffering lockOwned(Long id) {
        Long providerId = offerings.findProviderId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
        currentUser.requireOwnerOrAdmin(providerId);
        users.findByIdForUpdate(providerId).orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
        return offerings.findByIdForUpdate(id).filter(ServiceOffering::isActive)
            .filter(s -> s.getProvider().isActive())
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
    }

    private ServiceOffering visibleEntity(Long id) {
        return offerings.findVisibleById(id).orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
    }

    private static Specification<ServiceOffering> visible() {
        return (root, query, cb) -> cb.and(cb.isTrue(root.get("active")),
            cb.isTrue(root.get("provider").get("active")), cb.equal(root.get("provider").get("userType"), UserType.PROVIDER));
    }

    private static Pageable safePage(Pageable pageable) {
        return Paging.safe(pageable, Set.of("id", "title", "price", "category", "createdAt", "updatedAt"));
    }

    private void copy(ServiceOfferingRequest request, ServiceOffering offering) {
        String title = sanitizer.plainText(request.title());
        String description = sanitizer.optionalText(request.description());
        if (title == null || title.isBlank() || title.length() > 150)
            throw new IllegalArgumentException("Título é obrigatório e deve ter no máximo 150 caracteres após sanitização");
        if (description != null && description.length() > 5000)
            throw new IllegalArgumentException("Descrição deve ter no máximo 5000 caracteres");
        offering.setTitle(title); offering.setDescription(description);
        offering.setPrice(request.price()); offering.setCategory(request.category());
    }
}
