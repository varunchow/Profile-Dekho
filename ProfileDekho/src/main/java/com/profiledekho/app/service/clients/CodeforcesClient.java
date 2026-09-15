package com.profiledekho.app.service.clients;

import com.profiledekho.app.dto.PlatformStats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class CodeforcesClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CodeforcesClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://codeforces.com/api").build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch Codeforces statistics for a user
     */
    public Mono<PlatformStats> fetchStats(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return Mono.just(PlatformStats.builder()
                    .platform("codeforces")
                    .valid(false)
                    .error("empty_handle")
                    .build());
        }

        return Mono.zip(
                fetchUserInfo(handle),
                fetchUserRating(handle)
        )
        .map(tuple -> {
            JsonNode userInfo = tuple.getT1();
            JsonNode ratingData = tuple.getT2();
            return buildStats(userInfo, ratingData);
        })
        .onErrorResume(e -> Mono.just(PlatformStats.builder()
                .platform("codeforces")
                .valid(false)
                .error("fetch_failed")
                .message(e.getMessage())
                .build()))
        .timeout(Duration.ofSeconds(5));
    }

    private Mono<JsonNode> fetchUserInfo(String handle) {
        return webClient.get()
                .uri("/user.info?handles={handle}", handle)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        if (root.has("status") && "OK".equals(root.get("status").asText())) {
                            return root.get("result").get(0);
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .onErrorReturn(null)
                .timeout(Duration.ofSeconds(5));
    }

    private Mono<JsonNode> fetchUserRating(String handle) {
        return webClient.get()
                .uri("/user.rating?handle={handle}", handle)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        if (root.has("status") && "OK".equals(root.get("status").asText())) {
                            return root.get("result");
                        }
                        return objectMapper.createArrayNode();
                    } catch (Exception e) {
                        return objectMapper.createArrayNode();
                    }
                })
                .onErrorReturn(objectMapper.createArrayNode())
                .timeout(Duration.ofSeconds(5));
    }

    private PlatformStats buildStats(JsonNode userInfo, JsonNode ratingHistory) {
        if (userInfo == null) {
            return PlatformStats.builder()
                    .platform("codeforces")
                    .valid(false)
                    .error("not_found")
                    .build();
        }

        int rating = userInfo.has("rating") ? userInfo.get("rating").asInt(0) : 0;
        int maxRating = userInfo.has("maxRating") ? userInfo.get("maxRating").asInt(0) : 0;
        String rank = userInfo.has("rank") ? userInfo.get("rank").asText("Unrated") : "Unrated";
        
        int contests = 0;
        if (ratingHistory != null && ratingHistory.isArray()) {
            contests = ratingHistory.size();
        }

        return PlatformStats.builder()
                .platform("codeforces")
                .valid(true)
                .solved(null) // Codeforces doesn't expose total solved count via API
                .rating(rating)
                .maxRating(maxRating)
                .rankName(rank)
                .contests(contests)
                .build();
    }
}
