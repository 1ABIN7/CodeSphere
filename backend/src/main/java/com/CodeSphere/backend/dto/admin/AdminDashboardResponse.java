package com.CodeSphere.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Aggregated, administrator-only overview data. Keeping this in one response
 * prevents the client from having to infer totals from partial list responses.
 */
@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {
    private final long totalAssessments;
    private final long pendingReviews;
    private final long activeSessions;
    private final long publishedQuestions;
    private final List<RecentActivity> recentActivity;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentActivity {
        private final Long id;
        private final String problemTitle;
        private final String candidateName;
        private final String language;
        private final String status;
        private final OffsetDateTime createdAt;
    }
}
