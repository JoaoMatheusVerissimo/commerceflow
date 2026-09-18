package com.commerceflow.inventory.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean io.swagger.v3.oas.models.OpenAPI openApi() {
        return new io.swagger.v3.oas.models.OpenAPI().info(new io.swagger.v3.oas.models.info.Info()
                .title("CommerceFlow Inventory API").version("v1"));
    }
}
