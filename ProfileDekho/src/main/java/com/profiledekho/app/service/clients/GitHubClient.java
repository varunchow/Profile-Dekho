package com.profiledekho.app.service.clients;

import com.profiledekho.app.dto.PlatformStats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class GitHubClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String personalAccessToken;

    public GitHubClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                        @Value("${github.personal-access-token:}") String personalAccessToken) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.objectMapper = objectMapper;
        this.personalAccessToken = personalAccessToken;
    }

    /**
     * Fetch GitHub user statistics
     */
    public Mono<PlatformStats> fetchStats(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Mono.just(PlatformStats.builder()
                    .platform("github")
                    .valid(false)
                    .error("empty_handle")
                    .build());
        }

        return Mono.zip(
                fetchUserInfo(username),
                fetchRepositories(username),
                fetchContributions(username)
        )
        .map(tuple -> {
            JsonNode userInfo = tuple.getT1();
            JsonNode repos = tuple.getT2();
            JsonNode contributions = tuple.getT3();
            return buildStats(userInfo, repos, contributions);
        })
        .onErrorResume(e -> Mono.just(PlatformStats.builder()
                .platform("github")
                .valid(false)
                .error("fetch_failed")
                .message(e.getMessage())
                .build()))
        .timeout(Duration.ofSeconds(5));
    }

    private Mono<JsonNode> fetchUserInfo(String username) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri("/users/{username}", username)
                .header("Accept", "application/vnd.github.v3+json");
        
        if (personalAccessToken != null && !personalAccessToken.isEmpty()) {
            spec = spec.header("Authorization", "token " + personalAccessToken);
        }

        return spec
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        return objectMapper.readTree(response);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .onErrorReturn(null)
                .timeout(Duration.ofSeconds(5));
    }

    private Mono<JsonNode> fetchRepositories(String username) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri("/users/{username}/repos?type=owner&per_page=100", username)
                .header("Accept", "application/vnd.github.v3+json");
        
        if (personalAccessToken != null && !personalAccessToken.isEmpty()) {
            spec = spec.header("Authorization", "token " + personalAccessToken);
        }

        return spec
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode repos = objectMapper.readTree(response);
                        return repos.isArray() ? repos : objectMapper.createArrayNode();
                    } catch (Exception e) {
                        return objectMapper.createArrayNode();
                    }
                })
                .onErrorReturn(objectMapper.createArrayNode())
                .timeout(Duration.ofSeconds(5));
    }

    private Mono<JsonNode> fetchContributions(String username) {
        // GitHub contributions API is limited without personal token
        // This is a placeholder that returns contributions for the year
        return Mono.just(objectMapper.createObjectNode())
                .timeout(Duration.ofSeconds(5));
    }

    private PlatformStats buildStats(JsonNode userInfo, JsonNode repos, JsonNode contributions) {
        if (userInfo == null || userInfo.isMissingNode()) {
            return PlatformStats.builder()
                    .platform("github")
                    .valid(false)
                    .error("not_found")
                    .build();
        }

        int publicRepos = userInfo.path("public_repos").asInt(0);
        int followers = userInfo.path("followers").asInt(0);
        int stars = 0;

        if (repos != null && repos.isArray()) {
            for (JsonNode repo : repos) {
                stars += repo.path("stargazers_count").asInt(0);
            }
        }

        return PlatformStats.builder()
                .platform("github")
                .valid(true)
                .solved(publicRepos)
                .stars(stars)
                .contests(followers)
                .build();
    }
}
