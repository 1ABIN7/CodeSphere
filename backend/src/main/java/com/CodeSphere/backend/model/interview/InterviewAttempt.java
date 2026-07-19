package com.CodeSphere.backend.model.interview;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * Records a user's answer to a single question within an interview session.
 */
@Entity
@Table(name = "interview_attempts", indexes = {
    @Index(name = "idx_iattempt_session",  columnList = "session_id"),
    @Index(name = "idx_iattempt_question", columnList = "question_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    @Builder.Default
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds = 0;

    @Builder.Default
    @Column
    private Integer score = 0;

    @Builder.Default
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (attemptedAt == null) attemptedAt = OffsetDateTime.now();
    }
}
