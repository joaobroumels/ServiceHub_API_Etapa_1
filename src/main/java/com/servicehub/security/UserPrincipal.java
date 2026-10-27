package com.servicehub.security;

import com.servicehub.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public record UserPrincipal(User user) implements UserDetails {
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
    }
    public String getPassword() { return user.getPasswordHash(); }
    public String getUsername() { return user.getEmail(); }
    public boolean isEnabled() { return user.isActive(); }
    public Long getId() { return user.getId(); }
}
