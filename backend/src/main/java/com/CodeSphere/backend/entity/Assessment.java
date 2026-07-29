package com.CodeSphere.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assessment_type", nullable = false)
    private String assessmentType; // CODING, MCQ, WRITTEN, etc.

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "passing_score", columnDefinition = "numeric")
    private Double passingScore;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions;

    @Column(name = "allow_resume", nullable = false)
    private boolean allowResume;

    @Column(name = "is_certifying", nullable = false)
    private boolean isCertifying;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @Column(name = "access_code")
    private String accessCode;

    @Builder.Default
    @Column(name = "results_visible", nullable = false)
    private boolean resultsVisible = true;

    @Builder.Default
    @Column(name = "feedback_visible", nullable = false)
    private boolean feedbackVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
