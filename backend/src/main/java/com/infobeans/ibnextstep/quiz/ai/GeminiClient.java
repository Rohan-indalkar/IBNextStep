package com.infobeans.ibnextstep.quiz.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around Gemini's REST API. Nothing above this layer (Quiz
 * Service, Controller) ever talks to Gemini directly — matches the required
 * Controller -> Quiz Service -> AI Quiz Service -> Gemini API layering.
 *
 * Requires app.gemini.api-key to be set in application.properties. Without
 * it, every call fails clearly with a BadRequestException rather than a
 * confusing null-pointer or silent failure.
 */
@Component
@Slf4j
public class GeminiClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    
    /** Sends one prompt, returns Gemini's raw text response (expected to be a JSON string per our prompt instructions — parsed by the caller). */
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.gemini.api-key is not configured. Set it in application.properties to enable AI quiz generation.");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "contents", java.util.List.of(
                            java.util.Map.of("parts", java.util.List.of(
                                    java.util.Map.of("text", prompt)
                            ))
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(ENDPOINT_TEMPLATE, model, apiKey)))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API returned {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode()) {
                log.error("Unexpected Gemini response shape: {}", response.body());
                throw new RuntimeException("Could not extract text from Gemini response");
            }

            return textNode.asText();

        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
}
