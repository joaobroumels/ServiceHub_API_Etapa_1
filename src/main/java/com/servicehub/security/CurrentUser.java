package com.servicehub.security;

import com.servicehub.model.User;
import com.servicehub.model.UserType;
import com.servicehub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }
    public User user() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Autenticação necessária");
        }
        return users.findByIdAndActiveTrue(principal.getId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuário inativo"));
    }
    public Long id() { return user().getId(); }
    public boolean isAdmin() { return user().getUserType() == UserType.ADMIN; }
    public void requireOwnerOrAdmin(Long ownerId) {
        User actor = user();
        if (!actor.getId().equals(ownerId) && actor.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException("Você não pode acessar este recurso");
        }
    }
}
