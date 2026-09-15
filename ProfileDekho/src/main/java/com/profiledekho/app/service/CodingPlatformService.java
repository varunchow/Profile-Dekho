package com.profiledekho.app.service;

import com.profiledekho.app.dto.PlatformStats;
import com.profiledekho.app.model.UserProfile;
import com.profiledekho.app.service.clients.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CodingPlatformService {

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,30}$");

    @Autowired
    private CodeforcesClient codeforcesClient;

    @Autowired
    private LeetCodeClient leetcodeClient;

    @Autowired
    private CodeChefClient codechefClient;

    @Autowired
    private GitHubClient githubClient;

    @Autowired
    private InterviewBitClient interviewbitClient;

    public UserProfile fetchAndAggregateStats(
            String username,
            String leetcode,
            String codeforces,
            String codechef,
            String hackerrank,
            String interviewbit,
            String github
    ) {
        // Build initial profile
        UserProfile profile = new UserProfile();
        profile.setUsername(username != null && !username.trim().isEmpty() ? username : "coder");
        profile.setName(username != null ? capitalize(username) : "Anonymous Coder");
        profile.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + profile.getUsername());
        profile.setBio("Competitive Programmer & Software Developer | ProfileDekho Showcase");
        profile.setLastFetchedAt(Instant.now().toString());

        // Validate handles
        String lcHandle = validateHandle(leetcode);
        String cfHandle = validateHandle(codeforces);
        String ccHandle = validateHandle(codechef);
        String ibHandle = validateHandle(interviewbit);
        String ghHandle = validateHandle(github);

        // Set handles in profile
        profile.setLeetcodeHandle(lcHandle != null ? lcHandle : "");
        profile.setCodeforcesHandle(cfHandle != null ? cfHandle : "");
        profile.setCodechefHandle(ccHandle != null ? ccHandle : "");
        profile.setinterviewbitHandle(ibHandle != null ? ibHandle : "");
        profile.setGithubHandle(ghHandle != null ? ghHandle : "");
        profile.setHackerrankHandle(""); // deprecated

        // Fetch all platforms in parallel with 5-second timeout each
        // Each platform failure is independent and won't block others
        Mono<PlatformStats> leetcodeMono = lcHandle != null 
                ? leetcodeClient.fetchStats(lcHandle) 
                : Mono.just(PlatformStats.builder().platform("leetcode").valid(false).error("invalid_handle").build());

        Mono<PlatformStats> codeforceMono = cfHandle != null 
                ? codeforcesClient.fetchStats(cfHandle) 
                : Mono.just(PlatformStats.builder().platform("codeforces").valid(false).error("invalid_handle").build());

        Mono<PlatformStats> codechefMono = ccHandle != null 
                ? codechefClient.fetchStats(ccHandle) 
                : Mono.just(PlatformStats.builder().platform("codechef").valid(false).error("invalid_handle").build());

        Mono<PlatformStats> interviewbitMono = ibHandle != null 
                ? interviewbitClient.fetchStats(ibHandle) 
                : Mono.just(PlatformStats.builder().platform("interviewbit").valid(false).error("invalid_handle").build());

        Mono<PlatformStats> githubMono = ghHandle != null 
                ? githubClient.fetchStats(ghHandle) 
                : Mono.just(PlatformStats.builder().platform("github").valid(false).error("invalid_handle").build());

        // Execute all in parallel
        try {
            Mono.zip(leetcodeMono, codeforceMono, codechefMono, interviewbitMono, githubMono)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(); // Blocking call to wait for all results
        } catch (Exception e) {
            // Timeout or error - continue with partial data
            System.err.println("Error fetching platform stats: " + e.getMessage());
        }

        // Now fetch and aggregate results
        try {
            PlatformStats lcStats = lcHandle != null 
                    ? leetcodeClient.fetchStats(lcHandle).block(java.time.Duration.ofSeconds(6))
                    : PlatformStats.builder().platform("leetcode").valid(false).build();
            
            PlatformStats cfStats = cfHandle != null 
                    ? codeforcesClient.fetchStats(cfHandle).block(java.time.Duration.ofSeconds(6))
                    : PlatformStats.builder().platform("codeforces").valid(false).build();
            
            PlatformStats ccStats = ccHandle != null 
                    ? codechefClient.fetchStats(ccHandle).block(java.time.Duration.ofSeconds(6))
                    : PlatformStats.builder().platform("codechef").valid(false).build();
            
            PlatformStats ibStats = ibHandle != null 
                    ? interviewbitClient.fetchStats(ibHandle).block(java.time.Duration.ofSeconds(6))
                    : PlatformStats.builder().platform("interviewbit").valid(false).build();
            
            PlatformStats ghStats = ghHandle != null 
                    ? githubClient.fetchStats(ghHandle).block(java.time.Duration.ofSeconds(6))
                    : PlatformStats.builder().platform("github").valid(false).build();

            // Aggregate stats
            aggregateStats(profile, lcStats, cfStats, ccStats, ibStats, ghStats);

        } catch (Exception e) {
            // If all clients fail, use empty stats
            System.err.println("Error aggregating stats: " + e.getMessage());
        }
        return profile;
    }

    private String validateHandle(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return null;
        }
        String trimmed = handle.trim();
        if (trimmed.equalsIgnoreCase("v") || !HANDLE_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    private void aggregateStats(UserProfile profile, 
            PlatformStats lcStats, 
            PlatformStats cfStats, 
            PlatformStats ccStats, 
            PlatformStats ibStats, 
            PlatformStats ghStats) {

        int totalSolved = 0;
        int easySolved = 0;
        int mediumSolved = 0;
        int hardSolved = 0;
        int totalContests = 0;
        int maxRating = 0;
        int currentRating = 0;

        // Store individual stats
        Map<String, Object> lcMap = new HashMap<>();
        Map<String, Object> cfMap = new HashMap<>();
        Map<String, Object> ccMap = new HashMap<>();
        Map<String, Object> ibMap = new HashMap<>();
        Map<String, Object> ghMap = new HashMap<>();

        // Process LeetCode
        if (lcStats != null && lcStats.getValid()) {
            lcMap.put("solved", lcStats.getSolved() != null ? lcStats.getSolved() : 0);
            lcMap.put("easy", lcStats.getEasy() != null ? lcStats.getEasy() : 0);
            lcMap.put("medium", lcStats.getMedium() != null ? lcStats.getMedium() : 0);
            lcMap.put("hard", lcStats.getHard() != null ? lcStats.getHard() : 0);
            lcMap.put("ranking", lcStats.getRanking() != null ? lcStats.getRanking() : 0);
            lcMap.put("valid", true);

            totalSolved += lcStats.getSolved() != null ? lcStats.getSolved() : 0;
            easySolved += lcStats.getEasy() != null ? lcStats.getEasy() : 0;
            mediumSolved += lcStats.getMedium() != null ? lcStats.getMedium() : 0;
            hardSolved += lcStats.getHard() != null ? lcStats.getHard() : 0;
            totalContests += lcStats.getRanking() != null && lcStats.getRanking() > 0 ? 1 : 0;
        } else {
            lcMap.put("solved", 0);
            lcMap.put("valid", false);
        }
        profile.setLeetcodeStats(lcMap);

        // Process Codeforces
        if (cfStats != null && cfStats.getValid()) {
            cfMap.put("solved", cfStats.getSolved() != null ? cfStats.getSolved() : 0);
            cfMap.put("rating", cfStats.getRating() != null ? cfStats.getRating() : 0);
            cfMap.put("maxRating", cfStats.getMaxRating() != null ? cfStats.getMaxRating() : 0);
            cfMap.put("rankName", cfStats.getRankName() != null ? cfStats.getRankName() : "Unrated");
            cfMap.put("contests", cfStats.getContests() != null ? cfStats.getContests() : 0);
            cfMap.put("valid", true);

            totalSolved += cfStats.getSolved() != null ? cfStats.getSolved() : 0;
            totalContests += cfStats.getContests() != null ? cfStats.getContests() : 0;
            maxRating = Math.max(maxRating, cfStats.getMaxRating() != null ? cfStats.getMaxRating() : 0);
            currentRating = Math.max(currentRating, cfStats.getRating() != null ? cfStats.getRating() : 0);
        } else {
            cfMap.put("solved", 0);
            cfMap.put("valid", false);
        }
        profile.setCodeforcesStats(cfMap);

        // Process CodeChef
        if (ccStats != null && ccStats.getValid()) {
            ccMap.put("solved", ccStats.getSolved() != null ? ccStats.getSolved() : 0);
            ccMap.put("rating", ccStats.getRating() != null ? ccStats.getRating() : 0);
            ccMap.put("maxRating", ccStats.getMaxRating() != null ? ccStats.getMaxRating() : 0);
            ccMap.put("contests", ccStats.getContests() != null ? ccStats.getContests() : 0);
            ccMap.put("valid", true);

            totalSolved += ccStats.getSolved() != null ? ccStats.getSolved() : 0;
            totalContests += ccStats.getContests() != null ? ccStats.getContests() : 0;
            maxRating = Math.max(maxRating, ccStats.getMaxRating() != null ? ccStats.getMaxRating() : 0);
            currentRating = Math.max(currentRating, ccStats.getRating() != null ? ccStats.getRating() : 0);
        } else {
            ccMap.put("solved", 0);
            ccMap.put("valid", false);
        }
        profile.setCodechefStats(ccMap);

        // Process InterviewBit
        if (ibStats != null && ibStats.getValid()) {
            ibMap.put("solved", ibStats.getSolved() != null ? ibStats.getSolved() : 0);
            ibMap.put("contests", ibStats.getContests() != null ? ibStats.getContests() : 0);
            ibMap.put("ranking", ibStats.getRanking() != null ? ibStats.getRanking() : 0);
            ibMap.put("private", ibStats.getPrivate_() != null ? ibStats.getPrivate_() : false);
            ibMap.put("valid", true);

            totalSolved += ibStats.getSolved() != null ? ibStats.getSolved() : 0;
            totalContests += ibStats.getContests() != null ? ibStats.getContests() : 0;
        } else if (ibStats != null && ibStats.getPrivate_() != null && ibStats.getPrivate_()) {
            ibMap.put("valid", false);
            ibMap.put("private", true);
            ibMap.put("message", "Profile is private");
        } else {
            ibMap.put("solved", 0);
            ibMap.put("valid", false);
        }
        profile.setinterviewbitStats(ibMap);

        // Process GitHub
        if (ghStats != null && ghStats.getValid()) {
            ghMap.put("publicRepos", ghStats.getSolved() != null ? ghStats.getSolved() : 0);
            ghMap.put("stars", ghStats.getStars() != null ? ghStats.getStars() : 0);
            ghMap.put("followers", ghStats.getContests() != null ? ghStats.getContests() : 0);
            ghMap.put("valid", true);
        } else {
            ghMap.put("publicRepos", 0);
            ghMap.put("valid", false);
        }
        profile.setGithubStats(ghMap);

        // Deprecated - keep empty
        Map<String, Object> hrStats = new HashMap<>();
        hrStats.put("solved", 0);
        hrStats.put("valid", false);
        profile.setHackerrankStats(hrStats);

        // Set Aggregated Metrics
        profile.setTotalSolved(totalSolved);
        profile.setEasySolved(easySolved);
        profile.setMediumSolved(mediumSolved);
        profile.setHardSolved(hardSolved);
        profile.setTotalContests(totalContests);
        profile.setMaxRating(maxRating);
        profile.setCurrentRating(currentRating);

        int calculatedScore = (easySolved * 10) + (mediumSolved * 25) + (hardSolved * 50);
        profile.setGlobalScore(calculatedScore);

        // Set title based on max rating
        if (maxRating >= 2200) profile.setTitle("Grandmaster");
        else if (maxRating >= 1900) profile.setTitle("Master");
        else if (maxRating >= 1600) profile.setTitle("Candidate Master");
        else if (maxRating >= 1400) profile.setTitle("Specialist");
        else if (totalSolved > 0) profile.setTitle("Member");
        else profile.setTitle("Beginner");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
