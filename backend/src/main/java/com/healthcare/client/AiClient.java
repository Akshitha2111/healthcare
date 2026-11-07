package com.healthcare.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.config.AppProperties;
import com.healthcare.exception.ApiException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The only class that talks to the Flask AI microservice. It enforces connect/read timeouts and converts every
 * failure into an ApiException with a safe message. Symptoms are never logged.
 */
@Component
public class AiClient {
    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestClient rest;
    private final ObjectMapper mapper;

    public AiClient(AppProperties props, ObjectMapper mapper) {
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.ai().connectTimeoutMs());
        factory.setReadTimeout(props.ai().readTimeoutMs());
        RestClient.Builder builder = RestClient.builder().baseUrl(props.ai().baseUrl()).requestFactory(factory);
        String key = props.ai().apiKey();
        if (key != null && !key.isBlank()) {
            builder.defaultHeader("X-Internal-Api-Key", key);
        }
        this.rest = builder.build();
    }

    public AiPredictResponse predict(List<String> symptoms) {
        try {
            AiPredictResponse response = rest.post().uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("symptoms", symptoms))
                    .retrieve()
                    .body(AiPredictResponse.class);
            if (response == null || response.predictions() == null || response.predictions().isEmpty()
                    || response.symptomsUsed() == null) {
                throw ApiException.badGateway("The prediction service returned an unexpected response");
            }
            return response;
        } catch (RestClientException ex) {
            throw translate(ex);
        }
    }

    public AiSymptomCatalog symptoms() {
        try {
            AiSymptomCatalog catalog = rest.get().uri("/symptoms").retrieve().body(AiSymptomCatalog.class);
            if (catalog == null || catalog.symptoms() == null) {
                throw ApiException.badGateway("The prediction service returned an unexpected response");
            }
            return catalog;
        } catch (RestClientException ex) {
            throw translate(ex);
        }
    }

    /** Used by the readiness health indicator. */
    public boolean isHealthy() {
        try {
            rest.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            return false;
        }
    }

    private ApiException translate(RestClientException ex) {
        if (ex instanceof ResourceAccessException) { // connection refused, DNS failure, timeout
            log.warn("AI service unreachable or timed out: {}", ex.getClass().getSimpleName());
            return ApiException.unavailable(
                    "The prediction service is temporarily unavailable. Please try again shortly.");
        }
        if (ex instanceof HttpClientErrorException client) {
            if (client.getStatusCode().value() == 400) {
                return ApiException.badRequest(extractMessage(client.getResponseBodyAsString()));
            }
            log.error("AI service rejected the request with status {}", client.getStatusCode().value());
            return ApiException.badGateway("The prediction service rejected the request");
        }
        if (ex instanceof HttpServerErrorException server) {
            log.error("AI service failed with status {}", server.getStatusCode().value());
            if (server.getStatusCode().value() == 503) {
                return ApiException.unavailable("The prediction service is temporarily unavailable.");
            }
        } else {
            log.error("Unexpected AI client failure: {}", ex.getClass().getSimpleName());
        }
        return ApiException.badGateway("The prediction service returned an error");
    }

    private String extractMessage(String body) {
        try {
            JsonNode message = mapper.readTree(body).path("error").path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            // fall through to the generic message
        }
        return "Invalid symptoms";
    }
}
