package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.security.JwtService;
import com.servicehub.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final UserService users;
    private final InputSanitizer sanitizer;
    public AuthService(AuthenticationManager authenticationManager, JwtService jwt,
                       UserService users, InputSanitizer sanitizer) {
        this.authenticationManager = authenticationManager; this.jwt = jwt;
        this.users = users; this.sanitizer = sanitizer;
    }
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(sanitizer.email(request.email()), request.password()));
        var user = ((UserPrincipal) authentication.getPrincipal()).user();
        return new AuthResponse(jwt.generateToken(user), "Bearer", jwt.expirationSeconds(), UserResponse.from(user));
    }
    public UserResponse register(UserRequest request) { return users.register(request); }
}
