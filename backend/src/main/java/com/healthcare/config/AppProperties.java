package com.healthcare.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed view of the "app.*" settings in application.yml (all values come from environment variables). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String clinicTimezone,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cookie cookie,
        @NotNull Cors cors,
        @Valid @NotNull Ai ai,
        @NotNull RateLimit rateLimit,
        @NotNull Analytics analytics,
        @NotNull Admin admin,
        @NotNull Seed seed) {

    /** The secret must be at least 32 characters (256 bits) for HS256; startup fails otherwise. */
    public record Jwt(@NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
                      @Min(1) long expirationMinutes) {}

    public record Cookie(@NotBlank String name, boolean secure, @NotBlank String sameSite) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Ai(@NotBlank String baseUrl, String apiKey, @Min(100) int connectTimeoutMs,
                     @Min(100) int readTimeoutMs) {}

    public record RateLimit(boolean enabled, @Min(1) int authPerMinute, @Min(1) int predictionPerMinute) {}

    public record Analytics(@Min(1) int minGroupSize) {}

    public record Admin(String email, String password, String fullName) {}

    public record Seed(boolean demoDoctors) {}
}
