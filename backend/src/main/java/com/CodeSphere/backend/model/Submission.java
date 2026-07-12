package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a code submission by a user against a problem.
 *
 * Tracks the code, language, judge verdict, execution metrics,
 * per-test-case results, and AI-generated feedback.
 */
@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submissions_user_prob", columnList = "user_id, problem_id"),
    @Index(name = "idx_submissions_status", columnList = "status"),
    @Index(name = "idx_submissions_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false, length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "exec_time")
    private Integer execTime; // milliseconds

    @Column(name = "exec_memory")
    private Integer execMemory; // KB

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ---- V5 Extensions ----

    @Column
    @Builder.Default
    private Integer score = 0;

    @Column(name = "test_cases_passed")
    @Builder.Default
    private Integer testCasesPassed = 0;

    @Column(name = "total_test_cases")
    @Builder.Default
    private Integer totalTestCases = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_feedback", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> aiFeedback = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "complexity_analysis", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> complexityAnalysis = Map.of();

    // ---- Relationships ----

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SubmissionResult> results = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
