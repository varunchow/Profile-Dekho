package com.profiledekho.app.model;

import java.util.List;
import java.util.Map;

public class UserProfile {
    private String username;
    private String name;
    private String avatar;
    private String bio;
    private String title; // Grandmaster, Master, Knight, Specialist, etc.
    private int globalScore;
    
    // Coding platform handles
    private String leetcodeHandle;
    private String codeforcesHandle;
    private String codechefHandle;
    private String hackerrankHandle;
    private String interviewbitHandle;
    private String githubHandle;

    // Platform Specific Detailed Stats
    private Map<String, Object> leetcodeStats;
    private Map<String, Object> codeforcesStats;
    private Map<String, Object> codechefStats;
    private Map<String, Object> hackerrankStats;
    private Map<String, Object> interviewbitStats;
    private Map<String, Object> githubStats;

    // Aggregated Metrics
    private int totalSolved;
    private int easySolved;
    private int mediumSolved;
    private int hardSolved;
    private int totalContests;
    private int maxRating;
    private int currentRating;

    private Map<String, Integer> topicScores;
    private List<Map<String, Object>> ratingHistory;

    public UserProfile() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getGlobalScore() { return globalScore; }
    public void setGlobalScore(int globalScore) { this.globalScore = globalScore; }

    public String getLeetcodeHandle() { return leetcodeHandle; }
    public void setLeetcodeHandle(String leetcodeHandle) { this.leetcodeHandle = leetcodeHandle; }

    public String getCodeforcesHandle() { return codeforcesHandle; }
    public void setCodeforcesHandle(String codeforcesHandle) { this.codeforcesHandle = codeforcesHandle; }

    public String getCodechefHandle() { return codechefHandle; }
    public void setCodechefHandle(String codechefHandle) { this.codechefHandle = codechefHandle; }

    public String getHackerrankHandle() { return hackerrankHandle; }
    public void setHackerrankHandle(String hackerrankHandle) { this.hackerrankHandle = hackerrankHandle; }

    public String getInterviewbitHandle() { return interviewbitHandle; }
    public void setInterviewbitHandle(String interviewbitHandle) { this.interviewbitHandle = interviewbitHandle; }

    public String getGithubHandle() { return githubHandle; }
    public void setGithubHandle(String githubHandle) { this.githubHandle = githubHandle; }

    public Map<String, Object> getLeetcodeStats() { return leetcodeStats; }
    public void setLeetcodeStats(Map<String, Object> leetcodeStats) { this.leetcodeStats = leetcodeStats; }

    public Map<String, Object> getCodeforcesStats() { return codeforcesStats; }
    public void setCodeforcesStats(Map<String, Object> codeforcesStats) { this.codeforcesStats = codeforcesStats; }

    public Map<String, Object> getCodechefStats() { return codechefStats; }
    public void setCodechefStats(Map<String, Object> codechefStats) { this.codechefStats = codechefStats; }

    public Map<String, Object> getHackerrankStats() { return hackerrankStats; }
    public void setHackerrankStats(Map<String, Object> hackerrankStats) { this.hackerrankStats = hackerrankStats; }

    public Map<String, Object> getInterviewbitStats() { return interviewbitStats; }
    public void setInterviewbitStats(Map<String, Object> interviewbitStats) { this.interviewbitStats = interviewbitStats; }

    public Map<String, Object> getGithubStats() { return githubStats; }
    public void setGithubStats(Map<String, Object> githubStats) { this.githubStats = githubStats; }

    public int getTotalSolved() { return totalSolved; }
    public void setTotalSolved(int totalSolved) { this.totalSolved = totalSolved; }

    public int getEasySolved() { return easySolved; }
    public void setEasySolved(int easySolved) { this.easySolved = easySolved; }

    public int getMediumSolved() { return mediumSolved; }
    public void setMediumSolved(int mediumSolved) { this.mediumSolved = mediumSolved; }

    public int getHardSolved() { return hardSolved; }
    public void setHardSolved(int hardSolved) { this.hardSolved = hardSolved; }

    public int getTotalContests() { return totalContests; }
    public void setTotalContests(int totalContests) { this.totalContests = totalContests; }

    public int getMaxRating() { return maxRating; }
    public void setMaxRating(int maxRating) { this.maxRating = maxRating; }

    public int getCurrentRating() { return currentRating; }
    public void setCurrentRating(int currentRating) { this.currentRating = currentRating; }

    public Map<String, Integer> getTopicScores() { return topicScores; }
    public void setTopicScores(Map<String, Integer> topicScores) { this.topicScores = topicScores; }

    public List<Map<String, Object>> getRatingHistory() { return ratingHistory; }
    public void setRatingHistory(List<Map<String, Object>> ratingHistory) { this.ratingHistory = ratingHistory; }
}
