package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.*;
import com.servicehub.model.*;
import com.servicehub.repository.UserRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumSet;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser current;
    private final InputSanitizer sanitizer;
    private final ServiceRequestRepository requests;
    private final ServiceOfferingRepository offerings;
    public UserService(UserRepository users, PasswordEncoder encoder, CurrentUser current,
                       InputSanitizer sanitizer, ServiceRequestRepository requests, ServiceOfferingRepository offerings) {
        this.users = users; this.encoder = encoder; this.current = current;
        this.sanitizer = sanitizer; this.requests = requests;
        this.offerings = offerings;
    }
    @Transactional
    public UserResponse register(UserRequest request) {
        if (request.role() == UserType.ADMIN) {
            throw new BadRequestException("Cadastro público permite apenas CLIENT ou PROVIDER");
        }
        return persistNew(request);
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(UserRequest request) { return persistNew(request); }

    private UserResponse persistNew(UserRequest request) {
        String email = sanitizer.email(request.email());
        if (users.existsByEmail(email)) throw new DuplicateResourceException("Já existe um usuário com este e-mail");
        var user = new User();
        copyRequest(user, request);
        return UserResponse.from(users.saveAndFlush(user));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(users.findByActiveTrue(Paging.safe(pageable,
                Set.of("id", "fullName", "email", "userType", "createdAt", "updatedAt"))).map(UserResponse::from));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UserResponse me() { return UserResponse.from(current.user()); }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UserResponse findById(Long id) {
        current.requireOwnerOrAdmin(id);
        return UserResponse.from(findEntity(id));
    }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UserResponse update(Long id, UserRequest request) {
        current.requireOwnerOrAdmin(id);
        User user = lockActive(id);
        if (request.role() != user.getUserType()) {
            throw new BadRequestException("O papel não pode ser alterado pela atualização de perfil");
        }
        if (users.existsByEmailAndIdNot(sanitizer.email(request.email()), id)) {
            throw new DuplicateResourceException("Já existe outro usuário com este e-mail");
        }
        copyRequest(user, request);
        return UserResponse.from(users.saveAndFlush(user));
    }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void delete(Long id) {
        current.requireOwnerOrAdmin(id);
        User user = lockActive(id);
        if (requests.existsActiveForUser(id,
                EnumSet.of(RequestStatus.PENDING, RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS))) {
            throw new BusinessRuleException("Usuário possui contratações em andamento");
        }
        user.setActive(false);
        offerings.deactivateByProviderId(id);
        users.saveAndFlush(user);
    }
    private User findEntity(Long id) {
        return users.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
    private User lockActive(Long id) {
        return users.findByIdForUpdate(id).filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
    private void copyRequest(User user, UserRequest request) {
        String fullName = sanitizer.plainText(request.fullName());
        String bio = sanitizer.optionalText(request.bio());
        if (fullName.length() < 2 || fullName.length() > 100 || (bio != null && bio.length() > 2000)) {
            throw new BadRequestException("Nome ou biografia inválidos após sanitização");
        }
        user.setFullName(fullName);
        user.setEmail(sanitizer.email(request.email()));
        user.setPasswordHash(encoder.encode(request.password()));
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().strip());
        user.setUserType(request.role());
        user.setBio(bio);
        user.setAvatarUrl(request.avatarUrl() == null || request.avatarUrl().isBlank() ? null : request.avatarUrl().strip());
    }
}
