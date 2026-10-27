package com.servicehub.controller;

import com.servicehub.config.SecurityConfig;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.LoginRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.model.User;
import com.servicehub.model.UserType;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.*;
import com.servicehub.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Web slice: real MVC validation, security chain and error serialization; service isolated. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class,
        SecurityErrorWriter.class, CustomUserDetailsService.class})
class AuthControllerWebMvcTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService auth;
    @MockitoBean UserRepository users;

    @Test
    void shouldExposePublicLoginAndPreventCachingTokenResponse() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setFullName("Maria Silva");
        user.setEmail("maria@test.servicehub.com");
        user.setUserType(UserType.CLIENT);
        when(auth.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("signed-token", "Bearer", 86400, UserResponse.from(user)));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@test.servicehub.com","password":"Senha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn400ForInvalidLoginBeforeCallingBusinessService() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        verifyNoInteractions(auth);
    }

    @Test
    void shouldKeepSecurityFiltersEnabledForProtectedPathsAndMalformedBearer() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(post("/api/auth/login").header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@test.servicehub.com\",\"password\":\"Senha123\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(auth);
    }
}
