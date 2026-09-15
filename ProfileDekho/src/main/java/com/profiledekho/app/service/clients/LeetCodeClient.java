package com.profiledekho.app.service.clients;

import com.profiledekho.app.dto.PlatformStats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class LeetCodeClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public LeetCodeClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://leetcode.com").build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch LeetCode statistics for a user via GraphQL
     */
    public Mono<PlatformStats> fetchStats(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return Mono.just(PlatformStats.builder()
                    .platform("leetcode")
                    .valid(false)
                    .error("empty_handle")
                    .build());
        }

        String graphqlQuery = buildGraphQLQuery(handle);

        return webClient.post()
                .uri("/graphql")
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "operationName", "GetUserProfile",
                        "variables", Map.of("username", handle),
                        "query", graphqlQuery
                ))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> parseResponse(response, handle))
                .onErrorResume(e -> Mono.just(PlatformStats.builder()
                        .platform("leetcode")
                        .valid(false)
                        .error("fetch_failed")
                        .message(e.getMessage())
                        .build()))
                .timeout(Duration.ofSeconds(5));
    }

    private String buildGraphQLQuery(String username) {
        return "query GetUserProfile($username: String!) {\n" +
                "  matchedUser(username: $username) {\n" +
                "    username\n" +
                "    profile {\n" +
                "      ranking\n" +
                "    }\n" +
                "    submitStats {\n" +
                "      acSubmissionNum {\n" +
                "        difficulty\n" +
                "        count\n" +
                "        submissions\n" +
                "      }\n" +
                "      totalSubmissionNum {\n" +
                "        difficulty\n" +
                "        count\n" +
                "        submissions\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private PlatformStats parseResponse(String response, String handle) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Check for errors
            if (root.has("errors") && root.get("errors").isArray() && root.get("errors").size() > 0) {
                return PlatformStats.builder()
                        .platform("leetcode")
                        .valid(false)
                        .error("not_found")
                        .build();
            }

            JsonNode data = root.path("data").path("matchedUser");
            if (data == null || data.isMissingNode()) {
                return PlatformStats.builder()
                        .platform("leetcode")
                        .valid(false)
                        .error("not_found")
                        .build();
            }

            int ranking = data.path("profile").path("ranking").asInt(0);
            
            int easy = 0, medium = 0, hard = 0;
            JsonNode acStats = data.path("submitStats").path("acSubmissionNum");
            if (acStats.isArray()) {
                for (JsonNode stat : acStats) {
                    String difficulty = stat.path("difficulty").asText();
                    int count = stat.path("count").asInt(0);
                    
                    switch (difficulty) {
                        case "Easy":
                            easy = count;
                            break;
                        case "Medium":
                            medium = count;
                            break;
                        case "Hard":
                            hard = count;
                            break;
                    }
                }
            }

            int totalSolved = easy + medium + hard;

            return PlatformStats.builder()
                    .platform("leetcode")
                    .valid(true)
                    .solved(totalSolved)
                    .easy(easy)
                    .medium(medium)
                    .hard(hard)
                    .ranking(ranking)
                    .build();

        } catch (Exception e) {
            return PlatformStats.builder()
                    .platform("leetcode")
                    .valid(false)
                    .error("parse_error")
                    .message(e.getMessage())
                    .build();
        }
    }
}
