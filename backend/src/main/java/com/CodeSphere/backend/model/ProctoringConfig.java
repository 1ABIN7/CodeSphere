package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * Per-assessment configuration for proctoring constraints.
 */
@Entity
@Table(name = "proctoring_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProctoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false, unique = true)
    private Long assessmentId;

    @Builder.Default
    @Column(name = "enable_webcam")
    private boolean enableWebcam = true;

    @Builder.Default
    @Column(name = "enable_screen_recording")
    private boolean enableScreenRecording = false;

    @Builder.Default
    @Column(name = "enable_tab_switch_detection")
    private boolean enableTabSwitchDetection = true;

    @Builder.Default
    @Column(name = "enable_face_detection")
    private boolean enableFaceDetection = false;

    @Builder.Default
    @Column(name = "enable_audio_detection")
    private boolean enableAudioDetection = false;

    @Builder.Default
    @Column(name = "max_allowed_violations")
    private Integer maxAllowedViolations = 5;

    @Builder.Default
    @Column(name = "warning_threshold")
    private Integer warningThreshold = 3;

    @Builder.Default
    @Column(name = "auto_disqualify")
    private boolean autoDisqualify = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
