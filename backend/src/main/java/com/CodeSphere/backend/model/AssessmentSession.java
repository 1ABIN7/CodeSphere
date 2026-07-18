package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assessment_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, SUBMITTED, GRADING, GRADED, DISQUALIFIED

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "last_active_at")
    private OffsetDateTime lastActiveAt;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "max_possible_score")
    private Double maxPossibleScore;

    @Column(name = "current_section_index")
    @Builder.Default
    private Integer currentSectionIndex = 0;

    @Column(name = "proctoring_anomaly_score")
    @Builder.Default
    private Double proctoringAnomalyScore = 0.0;

    @Column(name = "disqualification_reason", columnDefinition = "TEXT")
    private String disqualificationReason;
}
