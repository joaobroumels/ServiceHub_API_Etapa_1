package com.servicehub.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.model.UserType;
import com.servicehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiIntegrationSupport {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;

    protected Session register(String email, String role) throws Exception {
        String response = performJson(post("/api/auth/register"), userPayload(email, role))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return new Session(objectMapper.readTree(response).get("id").asLong(), login(email), email);
    }

    protected String login(String email) throws Exception {
        String response = performJson(post("/api/auth/login"), Map.of("email", email, "password", "Senha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    protected Session administrator() throws Exception {
        Session registered = register("admin@test.servicehub.com", "CLIENT");
        var user = userRepository.findById(registered.id()).orElseThrow();
        user.setUserType(UserType.ADMIN);
        userRepository.saveAndFlush(user);
        return new Session(user.getId(), login(registered.email()), registered.email());
    }

    protected Map<String, Object> userPayload(String email, String role) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "Maria Silva");
        payload.put("email", email);
        payload.put("password", "Senha123");
        payload.put("phone", "62999999999");
        payload.put("role", role);
        return payload;
    }

    protected org.springframework.test.web.servlet.ResultActions performJson(
            MockHttpServletRequestBuilder request, Object payload) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    protected String bearer(Session session) { return "Bearer " + session.token(); }

    protected record Session(long id, String token, String email) {}
}
