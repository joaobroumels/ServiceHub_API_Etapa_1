package com.servicehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI serviceHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ServiceHub API")
                .description("API REST para gerenciamento de clientes, prestadores e serviços")
                .version("1.0.0")
                .contact(new Contact().name("Equipe ServiceHub")));
    }
}
