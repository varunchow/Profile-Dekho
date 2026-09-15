package com.profiledekho.app.service.clients;

import com.profiledekho.app.dto.PlatformStats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class CodeChefClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CodeChefClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://www.codechef.com/api/v2").build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch CodeChef statistics via unofficial API
     * Note: This is an unofficial API and may break. Results marked as best-effort.
     */
    public Mono<PlatformStats> fetchStats(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return Mono.just(PlatformStats.builder()
                    .platform("codechef")
                    .valid(false)
                    .error("empty_handle")
                    .message("Unofficial API - best effort basis")
                    .build());
        }

        return webClient.get()
                .uri("/users/{username}", handle)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> parseResponse(response))
                .onErrorResume(e -> Mono.just(PlatformStats.builder()
                        .platform("codechef")
                        .valid(false)
                        .error("fetch_failed")
                        .message("Unofficial API - " + e.getMessage())
                        .build()))
                .timeout(Duration.ofSeconds(5));
    }

    private PlatformStats parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Check if user exists
            if (!root.has("user") || root.path("user").isMissingNode()) {
                return PlatformStats.builder()
                        .platform("codechef")
                        .valid(false)
                        .error("not_found")
                        .build();
            }

            JsonNode user = root.path("user");
            
            int solved = user.path("numSuccessfulSubmissions").asInt(0);
            int rating = user.path("currentRating").asInt(0);
            int maxRating = user.path("highestRating").asInt(0);
            int ranking = user.path("globalRanking").asInt(0);
            int contests = user.path("numContestsParticipated").asInt(0);

            return PlatformStats.builder()
                    .platform("codechef")
                    .valid(true)
                    .solved(solved)
                    .rating(rating)
                    .maxRating(maxRating)
                    .globalRanking(ranking)
                    .contests(contests)
                    .build();

        } catch (Exception e) {
            return PlatformStats.builder()
                    .platform("codechef")
                    .valid(false)
                    .error("parse_error")
                    .message(e.getMessage())
                    .build();
        }
    }
}
