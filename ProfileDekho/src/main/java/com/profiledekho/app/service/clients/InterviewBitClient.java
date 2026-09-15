package com.profiledekho.app.service.clients;

import com.profiledekho.app.dto.PlatformStats;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class InterviewBitClient {

    private final WebClient webClient;

    public InterviewBitClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://www.interviewbit.com").build();
    }

    /**
     * Fetch InterviewBit statistics with privacy detection
     * Returns 4 states:
     * 1. Public + has data: {valid: true, private: false, solved: X, contests: Y, rank: Z}
     * 2. Public + no contests: {valid: true, private: false, solved: X, message: "No contests attempted yet"}
     * 3. Private: {valid: false, private: true}
     * 4. Not found: {valid: false, error: "not_found"}
     */
    public Mono<PlatformStats> fetchStats(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return Mono.just(PlatformStats.builder()
                    .platform("interviewbit")
                    .valid(false)
                    .error("empty_handle")
                    .build());
        }

        return webClient.get()
                .uri("/users/{username}", handle)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .retrieve()
                .bodyToMono(String.class)
                .map(html -> parseProfile(html))
                .onErrorResume(e -> Mono.just(PlatformStats.builder()
                        .platform("interviewbit")
                        .valid(false)
                        .error("fetch_failed")
                        .message(e.getMessage())
                        .build()))
                .timeout(Duration.ofSeconds(5));
    }

    private PlatformStats parseProfile(String html) {
        try {
            Document doc = Jsoup.parse(html);

            // Check for private profile - look for privacy message
            if (html.contains("private") || html.contains("This profile is private") || 
                html.contains("not public")) {
                return PlatformStats.builder()
                        .platform("interviewbit")
                        .valid(false)
                        .private_(true)
                        .build();
            }

            // Try to extract stats from the page
            Elements statsElements = doc.select("[class*='stat'], [class*='score'], [class*='solved']");
            
            if (statsElements.isEmpty()) {
                // Check if profile exists at all by looking for username
                Elements usernameElements = doc.select("[class*='username'], [class*='profile-name'], h1, h2");
                if (usernameElements.isEmpty()) {
                    return PlatformStats.builder()
                            .platform("interviewbit")
                            .valid(false)
                            .error("not_found")
                            .build();
                }
            }

            // Extract solved problems
            int solved = 0;
            Elements solvedElements = doc.select("span:containsOwn(Solved)").parents();
            for (Element el : solvedElements) {
                String text = el.text();
                if (text.contains("Solved")) {
                    try {
                        String[] parts = text.split("\\D+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                solved = Integer.parseInt(part);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Continue parsing
                    }
                }
            }

            // Extract contests participated
            int contests = 0;
            Elements contestElements = doc.select("span:containsOwn(Contests)").parents();
            for (Element el : contestElements) {
                String text = el.text();
                if (text.contains("Contests")) {
                    try {
                        String[] parts = text.split("\\D+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                contests = Integer.parseInt(part);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Continue parsing
                    }
                }
            }

            // Extract ranking
            int ranking = 0;
            Elements rankElements = doc.select("span:containsOwn(Rank)").parents();
            for (Element el : rankElements) {
                String text = el.text();
                if (text.contains("Rank")) {
                    try {
                        String[] parts = text.split("\\D+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                ranking = Integer.parseInt(part);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Continue parsing
                    }
                }
            }

            // Check if profile has no contests (state 2)
            if (contests == 0 && solved > 0) {
                return PlatformStats.builder()
                        .platform("interviewbit")
                        .valid(true)
                        .private_(false)
                        .solved(solved)
                        .contests(0)
                        .message("No contests attempted yet")
                        .build();
            }

            // State 1: Public with data
            return PlatformStats.builder()
                    .platform("interviewbit")
                    .valid(true)
                    .private_(false)
                    .solved(solved)
                    .contests(contests)
                    .ranking(ranking > 0 ? ranking : null)
                    .build();

        } catch (Exception e) {
            return PlatformStats.builder()
                    .platform("interviewbit")
                    .valid(false)
                    .error("parse_error")
                    .message(e.getMessage())
                    .build();
        }
    }
}
