package com.servicehub.controller;

import com.servicehub.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends ApiIntegrationSupport {
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterLoginAndReturnOnlyPublicUserData() throws Exception {
        Session client = register("maria@test.servicehub.com", "CLIENT");
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.id()))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        var stored = userRepository.findById(client.id()).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("Senha123");
        assertThat(passwordEncoder.matches("Senha123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void shouldDistinguishUnauthenticatedAndForbiddenRequests() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        Session client = register("client@test.servicehub.com", "CLIENT");
        Session other = register("other@test.servicehub.com", "CLIENT");
        mockMvc.perform(get("/api/users").header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/{id}", other.id()).header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());
        performJson(post("/api/users").header("Authorization", bearer(client)),
                userPayload("new@test.servicehub.com", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidDataWeakPasswordsAndDuplicateEmail() throws Exception {
        performJson(post("/api/auth/register"), Map.of(
                        "fullName", "", "email", "email-invalido", "password", "123", "phone", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        var weak = userPayload("weak@test.servicehub.com", "CLIENT");
        weak.put("password", "somenteletras");
        performJson(post("/api/auth/register"), weak).andExpect(status().isBadRequest());
        register("duplicate@test.servicehub.com", "CLIENT");
        performJson(post("/api/auth/register"), userPayload("DUPLICATE@test.servicehub.com", "CLIENT"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectPublicAdminRegistrationAndOwnerRoleEscalation() throws Exception {
        performJson(post("/api/auth/register"), userPayload("admin-attempt@test.servicehub.com", "ADMIN"))
                .andExpect(status().isBadRequest());
        Session client = register("client@test.servicehub.com", "CLIENT");
        performJson(put("/api/users/{id}", client.id()).header("Authorization", bearer(client)),
                userPayload(client.email(), "ADMIN"))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findById(client.id()).orElseThrow().getRole().name()).isEqualTo("CLIENT");
    }

    @Test
    void shouldUpdateOwnPhoneThenSoftDeleteAndInvalidateExistingToken() throws Exception {
        Session client = register("client@test.servicehub.com", "CLIENT");
        var update = userPayload(client.email(), "CLIENT");
        update.put("phone", "62988888888");
        performJson(put("/api/users/{id}", client.id()).header("Authorization", bearer(client)), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("62988888888"));
        mockMvc.perform(delete("/api/users/{id}", client.id()).header("Authorization", bearer(client)))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(client.id()).orElseThrow().isActive()).isFalse();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(client)))
                .andExpect(status().isUnauthorized());
        performJson(post("/api/auth/login"), Map.of("email", client.email(), "password", "Senha123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWrongPasswordAndTamperedToken() throws Exception {
        Session client = register("client@test.servicehub.com", "CLIENT");
        performJson(post("/api/auth/login"), Map.of("email", client.email(), "password", "Incorreta123"))
                .andExpect(status().isUnauthorized());
        int signatureStart = client.token().lastIndexOf('.') + 1;
        char replacement = client.token().charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = client.token().substring(0, signatureStart) + replacement
                + client.token().substring(signatureStart + 1);
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminToCreateUsersListPagesAndSeeNotFound() throws Exception {
        Session admin = administrator();
        performJson(post("/api/users").header("Authorization", bearer(admin)),
                userPayload("provider@test.servicehub.com", "PROVIDER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PROVIDER"));
        mockMvc.perform(get("/api/users?page=0&size=1").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldKeepLegacyUserTypeAndUsernameAliases() throws Exception {
        var payload = userPayload("legacy@test.servicehub.com", "CLIENT");
        payload.remove("role");
        payload.put("userType", "CLIENT");
        performJson(post("/api/auth/register"), payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userType").value("CLIENT"));
        performJson(post("/api/auth/login"), Map.of("username", "legacy@test.servicehub.com", "password", "Senha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void shouldSanitizeProfileTextAndRejectPasswordsExceedingBcryptByteLimit() throws Exception {
        var payload = userPayload("PROFILE@test.servicehub.com", "PROVIDER");
        payload.put("fullName", "<b>Maria Silva</b>");
        payload.put("bio", "<b>Profissional</b><script>alert(1)</script>");
        performJson(post("/api/auth/register"), payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Maria Silva"))
                .andExpect(jsonPath("$.bio").value("Profissional"))
                .andExpect(jsonPath("$.email").value("profile@test.servicehub.com"));
        var oversized = userPayload("oversized@test.servicehub.com", "CLIENT");
        oversized.put("password", "Á".repeat(36) + "A1"); // 38 caracteres, 74 bytes UTF-8.
        performJson(post("/api/auth/register"), oversized)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
