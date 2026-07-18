package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a user's session for taking an assessment.
 */
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

    // In a real system this would map to an Assessment entity, but we'll just use ID for now to satisfy the ProctoringService
    @Column(name = "assessment_id")
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User candidate;

    @Builder.Default
    @Column(name = "violation_count")
    private Integer violationCount = 0;
    
    @Column(name = "is_flagged")
    @Builder.Default
    private boolean isFlagged = false;
    
    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();
    
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

}
