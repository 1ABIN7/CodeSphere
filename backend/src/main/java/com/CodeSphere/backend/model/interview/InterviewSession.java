package com.CodeSphere.backend.model.interview;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single interview practice session for a user.
 * Contains a set of randomly selected questions from chosen categories.
 */
@Entity
@Table(name = "interview_sessions", indexes = {
    @Index(name = "idx_isess_user",   columnList = "user_id"),
    @Index(name = "idx_isess_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 50)
    @Builder.Default
    private InterviewSessionType sessionType = InterviewSessionType.PRACTICE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<Long> categoryIds = new ArrayList<>();

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private Integer totalQuestions = 10;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Builder.Default
    private Integer score = 0;

    @Column(name = "max_score")
    @Builder.Default
    private Integer maxScore = 0;

    @Column(name = "questions_answered")
    @Builder.Default
    private Integer questionsAnswered = 0;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InterviewSessionStatus status = InterviewSessionStatus.IN_PROGRESS;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (startedAt == null)  startedAt  = now;
        if (createdAt == null)  createdAt  = now;
    }
}
