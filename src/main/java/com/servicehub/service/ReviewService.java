package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.DuplicateResourceException;
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
import java.util.Set;

@Service
@Validated
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviews;
    private final ServiceRequestRepository requests;
    private final ServiceOfferingRepository offerings;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final InputSanitizer sanitizer;

    public ReviewService(ReviewRepository reviews, ServiceRequestRepository requests,
            ServiceOfferingRepository offerings, UserRepository users, CurrentUser currentUser, InputSanitizer sanitizer) {
        this.reviews = reviews; this.requests = requests; this.offerings = offerings;
        this.users = users; this.currentUser = currentUser; this.sanitizer = sanitizer;
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENT')")
    public ReviewResponse create(@Valid ReviewRequest input) {
        User reviewer = users.findByIdForUpdate(currentUser.id()).filter(User::isActive)
            .filter(u -> u.getUserType() == UserType.CLIENT)
            .orElseThrow(() -> new AccessDeniedException("Somente clientes ativos podem avaliar"));
        // Serializa avaliações concorrentes da mesma ordem; UNIQUE é a garantia final no banco.
        ServiceRequest request = requests.findByIdForUpdate(input.requestId())
            .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + input.requestId()));
        if (!request.getClient().getId().equals(reviewer.getId()))
            throw new AccessDeniedException("Somente o cliente da ordem pode avaliá-la");
        if (request.getStatus() != RequestStatus.COMPLETED)
            throw new BusinessRuleException("Apenas ordens concluídas podem ser avaliadas");
        if (reviews.existsByRequestId(request.getId()))
            throw new DuplicateResourceException("Esta ordem já possui uma avaliação");
        String comment = sanitizer.optionalText(input.comment());
        if (comment != null && comment.length() > 2000) throw new IllegalArgumentException("Comentário deve ter no máximo 2000 caracteres");
        Review review = new Review();
        review.setRequest(request); review.setReviewer(reviewer);
        review.setRating(input.rating().shortValue()); review.setComment(comment);
        return ReviewResponse.from(reviews.saveAndFlush(review));
    }

    public PageResponse<ReviewResponse> findByService(Long serviceId, Pageable pageable) {
        pageable = Paging.safe(pageable, Set.of("id", "rating", "createdAt"));
        if (offerings.findVisibleById(serviceId).isEmpty())
            throw new ResourceNotFoundException("Serviço não encontrado: " + serviceId);
        return PageResponse.from(reviews.findByServiceId(serviceId, pageable).map(ReviewResponse::from));
    }
}
