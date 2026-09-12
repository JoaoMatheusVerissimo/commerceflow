package com.commerceflow.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    io.swagger.v3.oas.models.OpenAPI openApi() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info().title("CommerceFlow Catalog API").version("v1"))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
    @Bean
    Clock clock() { return Clock.systemUTC(); }
}
