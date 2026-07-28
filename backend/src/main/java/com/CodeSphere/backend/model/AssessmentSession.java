package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Column(name = "assessment_id")
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User candidate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    private int durationMinutes;

    // --- Section & Question Tracking ---
    @ElementCollection
    @CollectionTable(name = "session_question_snapshots", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "question_id")
    @Builder.Default
    private List<Long> questionIdsSnapshot = new ArrayList<>();

    @Builder.Default
    @Column(name = "current_section_index")
    private int currentSectionIndex = 0;

    @Column(name = "current_section_started_at")
    private OffsetDateTime currentSectionStartedAt;

    @ElementCollection
    @CollectionTable(name = "session_completed_sections", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "section_index")
    @Builder.Default
    private Set<Integer> completedSectionIndexes = new HashSet<>();

    // --- Proctoring & Anti-Cheat Fields ---
    @Builder.Default
    @Column(name = "violation_count")
    private Integer violationCount = 0;

    @Builder.Default
    @Column(name = "is_flagged")
    private boolean isFlagged = false;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    public enum SessionStatus {
        IN_PROGRESS, SUBMITTED, TIMED_OUT
    }
}