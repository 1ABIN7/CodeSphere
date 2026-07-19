package com.CodeSphere.backend.model.interview;

import jakarta.persistence.*;
import lombok.*;

/**
 * Junction entity mapping questions to a specific interview session.
 */
@Entity
@Table(name = "interview_session_questions", indexes = {
    @Index(name = "idx_isq_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Builder.Default
    @Column(name = "is_answered")
    private boolean isAnswered = false;
}
