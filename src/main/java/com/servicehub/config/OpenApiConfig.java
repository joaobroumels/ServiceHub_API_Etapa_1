package com.servicehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI serviceHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ServiceHub API")
                .description("API REST para gerenciamento de clientes, prestadores e serviços")
                .version("2.0.0")
                .contact(new Contact().name("Equipe ServiceHub")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer publicEndpointsWithoutBearer() {
        return api -> api.getPaths().forEach((path, item) -> {
            if (path.startsWith("/api/auth/")) {
                item.readOperations().forEach(operation -> operation.setSecurity(List.of()));
            } else if (path.startsWith("/api/services") || path.startsWith("/api/providers")
                    || path.equals("/api/hello")) {
                if (item.getGet() != null) item.getGet().setSecurity(List.of());
            }
        });
    }
}
