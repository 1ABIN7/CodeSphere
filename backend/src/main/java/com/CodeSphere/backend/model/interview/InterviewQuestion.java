package com.CodeSphere.backend.model.interview;

import com.CodeSphere.backend.model.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single question in the interview preparation bank.
 *
 * Supports MCQ, fill-in-blank, true/false, short answer, coding snippets,
 * and case study scenarios. All question types store their answer in
 * {@code correctAnswer} — for MCQ, this is the option ID; for others, the text.
 */
@Entity
@Table(name = "interview_questions", indexes = {
    @Index(name = "idx_iq_category",   columnList = "category_id"),
    @Index(name = "idx_iq_difficulty", columnList = "difficulty"),
    @Index(name = "idx_iq_type",       columnList = "question_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private InterviewCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private InterviewQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(length = 200)
    private String topic;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * JSON array of options for MCQ / TRUE_FALSE questions.
     * Format: [{"id": "A", "text": "Option text"}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Object> options = new ArrayList<>();

    /** The correct answer. For MCQ: option ID (e.g. "A"). For others: answer text. */
    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "time_limit_seconds")
    @Builder.Default
    private Integer timeLimitSeconds = 120;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> hints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "company_tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> companyTags = new ArrayList<>();

    @Column(name = "created_by")
    private Long createdBy;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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
