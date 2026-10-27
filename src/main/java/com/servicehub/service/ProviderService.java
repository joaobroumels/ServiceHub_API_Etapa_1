package com.servicehub.service;

import com.servicehub.dto.PageResponse;
import com.servicehub.dto.ProviderResponse;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.UserType;
import com.servicehub.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProviderService {
    private final UserRepository users;
    public ProviderService(UserRepository users) { this.users = users; }

    public PageResponse<ProviderResponse> findAll(Pageable pageable) {
        return PageResponse.from(users.findByUserTypeAndActiveTrue(UserType.PROVIDER,
            Paging.safe(pageable, Set.of("id", "fullName"))).map(ProviderResponse::from));
    }

    public ProviderResponse findById(Long id) {
        return users.findByIdAndActiveTrue(id).filter(u -> u.getUserType() == UserType.PROVIDER)
            .map(ProviderResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado: " + id));
    }
}
