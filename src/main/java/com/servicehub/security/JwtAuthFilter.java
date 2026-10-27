package com.servicehub.security;

import com.servicehub.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final SecurityErrorWriter errors;
    public JwtAuthFilter(JwtService jwt, UserRepository users, SecurityErrorWriter errors) {
        this.jwt = jwt; this.users = users; this.errors = errors;
    }
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ") || header.length() > 8192) throw new IllegalArgumentException();
                var claims = jwt.parse(header.substring(7));
                var user = users.findByIdAndActiveTrue(Long.valueOf(claims.getSubject()))
                        .orElseThrow(IllegalArgumentException::new);
                // Recarrega situação e role: excluir ou mudar privilégios invalida o token anterior.
                if (!List.of("ROLE_" + user.getUserType().name()).equals(claims.get("roles"))) {
                    throw new IllegalArgumentException();
                }
                var principal = new UserPrincipal(user);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                errors.write(request, response, HttpStatus.UNAUTHORIZED, "Token inválido, expirado ou usuário inativo");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
