package com.servicehub.config;

import com.servicehub.model.User;
import com.servicehub.model.UserType;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.InputSanitizer;
import com.servicehub.validation.StrongPasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final InputSanitizer sanitizer;
    private final String email;
    private final String password;
    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, InputSanitizer sanitizer,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.sanitizer = sanitizer;
        this.email = email; this.password = password;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank() || email.length() > 255
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || !new StrongPasswordValidator().isValid(password, null)) {
            throw new IllegalArgumentException("Configure e-mail e senha forte para o administrador inicial");
        }
        String normalized = sanitizer.email(email);
        var existing = users.findByEmail(normalized);
        if (existing.isPresent()) {
            if (existing.get().getUserType() != UserType.ADMIN || !existing.get().isActive()) {
                throw new IllegalStateException("Bootstrap não pode promover ou reativar usuário existente");
            }
            return; // Nunca redefine senha nem promove contas durante a inicialização.
        }
        var user = new User();
        user.setFullName("Administrador ServiceHub");
        user.setEmail(normalized);
        user.setPasswordHash(encoder.encode(password));
        user.setUserType(UserType.ADMIN);
        users.saveAndFlush(user);
    }
}
