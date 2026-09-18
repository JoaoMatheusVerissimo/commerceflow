package com.commerceflow.inventory.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "commerceflow.inventory.demo-seed", havingValue = "true")
public class DemoInventorySeed implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    DemoInventorySeed(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void run(ApplicationArguments args) {
        var prefixes = List.of("BOTA-SERRA", "BOTA-HORIZONTE", "CHAPEU-CAMPO", "CHAPEU-TRILHA",
                "CINTO-ORIGEM", "CINTO-VEREDA");
        for (var prefix : prefixes) {
            for (var size : List.of("P", "M")) {
                jdbc.update("INSERT INTO stock_items(sku,physical,reserved,minimum,version,updated_at) "
                        + "VALUES (?,10,0,2,0,?) ON CONFLICT (sku) DO NOTHING",
                        prefix + "-" + size, OffsetDateTime.now(ZoneOffset.UTC));
            }
        }
    }
}
