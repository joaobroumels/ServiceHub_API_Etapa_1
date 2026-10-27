package com.servicehub.graphql;

import com.servicehub.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GraphQlIntegrationTest extends ApiIntegrationSupport {
    @Test
    void shouldQueryPublicProvidersWithTheirServices() throws Exception {
        Session provider = register("provider@test.servicehub.com", "PROVIDER");
        long serviceId = service(provider);
        String body = graphql("{ providers(first: 5) { id fullName services { id title price provider { id } } } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.providers[0].id").value(Long.toString(provider.id())))
                .andExpect(jsonPath("$.data.providers[0].services[0].id").value(Long.toString(serviceId)))
                .andExpect(jsonPath("$.data.providers[0].services[0].price").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat(new BigDecimal(objectMapper.readTree(body).at("/data/providers/0/services/0/price").asText()))
                .isEqualByComparingTo("150.00");
        graphql("{ servicesByProvider(providerId: \"" + provider.id() + "\", first: 5) { id title } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.servicesByProvider[0].id").value(Long.toString(serviceId)));
    }

    @Test
    void shouldCreateGraphqlBookingUsingAuthenticatedClientAndRejectAnonymousMutation() throws Exception {
        Session provider = register("provider@test.servicehub.com", "PROVIDER");
        Session client = register("client@test.servicehub.com", "CLIENT");
        long serviceId = service(provider);
        String mutation = """
                mutation {
                  bookService(input: {serviceId: "%s", scheduledAt: "%s", notes: "GraphQL"}) {
                    id clientId providerId serviceId status totalPrice
                  }
                }
                """.formatted(serviceId, LocalDateTime.now().plusDays(7));
        graphql(mutation, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("UNAUTHORIZED"));
        graphql(mutation, provider)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("FORBIDDEN"));
        String body = graphql(mutation, client)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookService.clientId").value(Long.toString(client.id())))
                .andExpect(jsonPath("$.data.bookService.providerId").value(Long.toString(provider.id())))
                .andExpect(jsonPath("$.data.bookService.status").value("PENDING"))
                .andExpect(jsonPath("$.data.bookService.totalPrice").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat(new BigDecimal(objectMapper.readTree(body).at("/data/bookService/totalPrice").asText()))
                .isEqualByComparingTo("150.00");
    }

    @Test
    void shouldRejectUnboundedQueriesAndInvalidBookingDates() throws Exception {
        graphql("{ providers(first: 101) { id } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));
        Session client = register("client@test.servicehub.com", "CLIENT");
        graphql("mutation { bookService(input: {serviceId: \"1\", scheduledAt: \"invalid\"}) { id } }", client)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldBlockRecursivePublicExpansionByWeightedComplexityBelowDepthLimit() throws Exception {
        // Somente cinco níveis: o bloqueio deve vir da cardinalidade, não do limite de profundidade.
        String query = """
                {
                  providers(first: 5) {
                    services {
                      provider {
                        services { id }
                      }
                    }
                  }
                }
                """;
        String body = graphql(query, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").isString())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).at("/errors/0/message").asText())
                .containsIgnoringCase("complexity");
        graphql("{ providers(first: 1) { services { id } } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private ResultActions graphql(String query, Session session) throws Exception {
        var request = post("/graphql");
        if (session != null) request.header("Authorization", bearer(session));
        ResultActions initial = performJson(request, Map.of("query", query));
        var result = initial.andReturn();
        return result.getRequest().isAsyncStarted() ? mockMvc.perform(asyncDispatch(result)) : initial;
    }

    private long service(Session provider) throws Exception {
        String body = performJson(post("/api/services").header("Authorization", bearer(provider)), Map.of(
                        "title", "Limpeza residencial", "description", "Limpeza completa", "price", 150, "category", "LIMPEZA"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
