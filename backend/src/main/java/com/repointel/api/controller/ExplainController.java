package com.repointel.api.controller;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxies dependency-explanation requests to Gemini via the official
 * Google GenAI Java SDK — no CORS issues since the call is server-side.
 *
 * POST /api/explain
 * Body : { "groupId": "...", "artifactId": "...", "scope": "..." }
 * Returns: { "explanation": "..." }
 */
@RestController
@RequestMapping("/api")
public class ExplainController {

    private static final Logger log = LoggerFactory.getLogger(ExplainController.class);

    // Cache: one lookup per dependency per JVM session
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostMapping("/explain")
    public ResponseEntity<Map<String, String>> explain(@RequestBody Map<String, String> body) {
        String groupId    = body.getOrDefault("groupId",    "");
        String artifactId = body.getOrDefault("artifactId", "");
        String scope      = body.getOrDefault("scope",      "compile");

        if (groupId.isBlank() || artifactId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("explanation", "Missing groupId or artifactId"));
        }

        String cacheKey = groupId + ":" + artifactId;
        String cached   = cache.get(cacheKey);
        if (cached != null) {
            return ResponseEntity.ok(Map.of("explanation", cached));
        }

        String prompt = "In exactly 1-2 sentences (max 28 words), explain what the "
                + scope + " dependency \"" + groupId + ":" + artifactId
                + "\" does and why a Java or JavaScript project would use it. Be specific. No filler.";

        try {
            // Use API key auth (set via env var GEMINI_API_KEY or passed directly)
            String geminiApiKey = "AIzaSyAHR5XRaJCyJk6kKjiwYiXgjHdJABjIv84";
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.0-flash-lite",
                    prompt,
                    null);

            String explanation = response.text() != null ? response.text().trim() : "No explanation available.";
            cache.put(cacheKey, explanation);
            log.debug("Gemini explained {}: {}", cacheKey, explanation);
            return ResponseEntity.ok(Map.of("explanation", explanation));

        } catch (Exception e) {
            log.warn("Gemini call failed for {}: {}", cacheKey, e.getMessage());
            return ResponseEntity.ok(Map.of("explanation", "Could not load explanation."));
        }
    }
}
