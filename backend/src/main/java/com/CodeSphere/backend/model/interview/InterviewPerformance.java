package com.CodeSphere.backend.model.interview;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * Tracks a user's performance statistics for a specific interview category.
 */
@Entity
@Table(name = "interview_performance", indexes = {
    @Index(name = "idx_iperf_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private InterviewCategory category;

    @Builder.Default
    @Column(name = "total_attempted")
    private Integer totalAttempted = 0;

    @Builder.Default
    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Builder.Default
    @Column(name = "total_sessions")
    private Integer totalSessions = 0;

    @Builder.Default
    @Column(name = "best_score")
    private Integer bestScore = 0;

    @Builder.Default
    @Column(name = "avg_time_seconds", columnDefinition = "numeric")
    private Double avgTimeSeconds = 0.0;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;
}
