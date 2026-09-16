package com.commerceflow.catalog.config;

import com.commerceflow.catalog.application.PricingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "commerceflow.catalog.demo-seed", havingValue = "true")
public class DemoCouponSeed implements ApplicationRunner {
    private final PricingService pricing;
    public DemoCouponSeed(PricingService pricing) { this.pricing = pricing; }
    @Override
    public void run(ApplicationArguments args) {
        if (pricing.coupons("DEMO10").isEmpty()) {
            pricing.save(new PricingService.Coupon("DEMO10", "PERCENT", new BigDecimal("10"),
                    new BigDecimal("100"), Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2030-01-01T00:00:00Z"), true, null),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"));
        }
    }
}
