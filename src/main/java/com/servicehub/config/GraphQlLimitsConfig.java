package com.servicehub.config;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQlLimitsConfig {
    @Bean
    MaxQueryDepthInstrumentation queryDepthLimit() { return new MaxQueryDepthInstrumentation(12); }
    @Bean
    MaxQueryComplexityInstrumentation queryComplexityLimit() {
        return new MaxQueryComplexityInstrumentation(1000, (environment, childComplexity) -> {
            String field = environment.getFieldDefinition().getName();
            int cardinality = switch (field) {
                case "providers", "servicesByProvider" -> {
                    Object first = environment.getArguments().get("first");
                    yield first instanceof Number number ? Math.max(1, Math.min(100, number.intValue())) : 20;
                }
                case "services" -> 20;
                default -> 1;
            };
            // O custo dos filhos é multiplicado pelo número de itens que o campo pode devolver.
            // Isso bloqueia ciclos Provider.services -> Service.provider -> Provider.services.
            return (int) Math.min(1001L, 1L + (long) cardinality * childComplexity);
        });
    }
}
