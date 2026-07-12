package com.codesphere.backend.model;

import java.util.List;
import java.util.Map;

public class UserDashboardDto {
    private ProfileDto profile;
    private long totalSubmissions;
    private long solvedQuestionsCount;
    private Map<String, Long> activityByDifficulty; // e.g., {"Easy": 5, "Medium": 12}

    public UserDashboardDto() {}

    public UserDashboardDto(ProfileDto profile, long totalSubmissions, long solvedQuestionsCount, Map<String, Long> activityByDifficulty) {
        this.profile = profile;
        this.totalSubmissions = totalSubmissions;
        this.solvedQuestionsCount = solvedQuestionsCount;
        this.activityByDifficulty = activityByDifficulty;
    }

    // Getters and Setters
    public ProfileDto getProfile() { return profile; }
    public void setProfile(ProfileDto profile) { this.profile = profile; }
    public long getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(long totalSubmissions) { this.totalSubmissions = totalSubmissions; }
    public long getSolvedQuestionsCount() { return solvedQuestionsCount; }
    public void setSolvedQuestionsCount(long solvedQuestionsCount) { this.solvedQuestionsCount = solvedQuestionsCount; }
    public Map<String, Long> getActivityByDifficulty() { return activityByDifficulty; }
    public void setActivityByDifficulty(Map<String, Long> activityByDifficulty) { this.activityByDifficulty = activityByDifficulty; }
}