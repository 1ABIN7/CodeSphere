package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a user's session for taking an assessment, tracking progress, and anti-cheating metrics.
 */
@Entity
@Table(name = "assessment_sessions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User candidate;

    // Field added to map username updates directly inside the session context
    @Column(name = "username")
    private String username;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "current_section_index")
    @Builder.Default
    private int currentSectionIndex = 0;

    @Column(name = "current_section_started_at")
    private LocalDateTime currentSectionStartedAt;

    // Proctoring & Integrity fields
    @Builder.Default
    @Column(name = "violation_count")
    private Integer violationCount = 0;

    @Column(name = "is_flagged")
    @Builder.Default
    private boolean isFlagged = false;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    // Dynamic snapshots for question sorting persistence
    @ElementCollection
    @CollectionTable(name = "session_question_snapshots", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "question_id")
    @Builder.Default
    private List<Long> questionIdsSnapshot = new ArrayList<>();

    // Section indexes tracking for sequential progress boundaries
    @ElementCollection
    @CollectionTable(name = "session_completed_sections", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "section_index")
    @Builder.Default
    private Set<Integer> completedSectionIndexes = new HashSet<>();

    public enum SessionStatus {
        IN_PROGRESS, SUBMITTED, TIMED_OUT
    }
}