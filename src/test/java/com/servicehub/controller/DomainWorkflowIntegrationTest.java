package com.servicehub.controller;

import com.servicehub.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DomainWorkflowIntegrationTest extends ApiIntegrationSupport {
    @Test
    void shouldCompleteBookingAndPermitExactlyOneReviewByItsClient() throws Exception {
        Session provider = register("provider@test.servicehub.com", "PROVIDER");
        Session client = register("client@test.servicehub.com", "CLIENT");
        Session stranger = register("stranger@test.servicehub.com", "CLIENT");
        long serviceId = service(provider);
        long requestId = booking(client, serviceId);

        mockMvc.perform(get("/api/requests/{id}", requestId).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        performJson(patch("/api/requests/{id}/status", requestId).header("Authorization", bearer(client)),
                Map.of("status", "ACCEPTED"))
                .andExpect(status().isForbidden());
        performJson(post("/api/reviews").header("Authorization", bearer(client)),
                Map.of("requestId", requestId, "rating", 5, "comment", "Excelente atendimento"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/{id}", provider.id()).header("Authorization", bearer(provider)))
                .andExpect(status().isConflict());

        for (String next : new String[]{"ACCEPTED", "IN_PROGRESS", "COMPLETED"}) {
            performJson(patch("/api/requests/{id}/status", requestId).header("Authorization", bearer(provider)),
                    Map.of("status", next)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next));
        }
        performJson(post("/api/reviews").header("Authorization", bearer(stranger)),
                Map.of("requestId", requestId, "rating", 5, "comment", "Não participei"))
                .andExpect(status().isForbidden());
        performJson(post("/api/reviews").header("Authorization", bearer(client)),
                Map.of("requestId", requestId, "rating", 5, "comment", "Excelente atendimento"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
        performJson(post("/api/reviews").header("Authorization", bearer(client)),
                Map.of("requestId", requestId, "rating", 4, "comment", "Duplicada"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/services/{id}/reviews", serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldEnforceProviderOwnershipAndValidateServiceInput() throws Exception {
        Session provider = register("provider@test.servicehub.com", "PROVIDER");
        Session other = register("other-provider@test.servicehub.com", "PROVIDER");
        Session client = register("client@test.servicehub.com", "CLIENT");
        long id = service(provider);
        mockMvc.perform(get("/api/services/{id}", id)).andExpect(status().isOk());
        performJson(post("/api/services").header("Authorization", bearer(client)), servicePayload())
                .andExpect(status().isForbidden());
        performJson(put("/api/services/{id}", id).header("Authorization", bearer(other)), servicePayload())
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/services/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
        performJson(post("/api/services").header("Authorization", bearer(provider)),
                Map.of("title", "", "price", -1, "category", "LIMPEZA"))
                .andExpect(status().isBadRequest());
        var update = new java.util.LinkedHashMap<>(servicePayload());
        update.put("price", 200);
        performJson(put("/api/services/{id}", id).header("Authorization", bearer(provider)), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(200));
        mockMvc.perform(delete("/api/services/{id}", id).header("Authorization", bearer(provider)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/services/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowClientCancellationAndRejectInvalidTransitionsOrPastDates() throws Exception {
        Session provider = register("provider@test.servicehub.com", "PROVIDER");
        Session client = register("client@test.servicehub.com", "CLIENT");
        long serviceId = service(provider);
        performJson(post("/api/requests").header("Authorization", bearer(client)), Map.of(
                "serviceId", serviceId, "scheduledAt", LocalDateTime.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest());
        long requestId = booking(client, serviceId);
        performJson(patch("/api/requests/{id}/status", requestId).header("Authorization", bearer(provider)),
                Map.of("status", "COMPLETED"))
                .andExpect(status().isConflict());
        performJson(patch("/api/requests/{id}/status", requestId).header("Authorization", bearer(client)),
                Map.of("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        performJson(patch("/api/requests/{id}/status", requestId).header("Authorization", bearer(provider)),
                Map.of("status", "ACCEPTED"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/{id}", client.id()).header("Authorization", bearer(client)))
                .andExpect(status().isNoContent());
    }

    private long service(Session provider) throws Exception {
        String response = performJson(post("/api/services").header("Authorization", bearer(provider)), servicePayload())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider.id").value(provider.id()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long booking(Session client, long serviceId) throws Exception {
        String response = performJson(post("/api/requests").header("Authorization", bearer(client)), Map.of(
                "serviceId", serviceId, "scheduledAt", LocalDateTime.now().plusDays(7).toString(), "notes", "Atendimento"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.clientId").value(client.id()))
                .andExpect(jsonPath("$.totalPrice").value(150))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Map<String, Object> servicePayload() {
        return Map.of("title", "Limpeza residencial", "description", "Limpeza com agendamento",
                "price", 150, "category", "LIMPEZA");
    }
}
