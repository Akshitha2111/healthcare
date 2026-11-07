package com.healthcare.client;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Part of /actuator/health and /actuator/health/readiness (indicator id "ai"). Not part of liveness. */
@Component("ai")
public class AiHealthIndicator implements HealthIndicator {
    private final AiClient client;

    public AiHealthIndicator(AiClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        return client.isHealthy() ? Health.up().build() : Health.down().build();
    }
}
