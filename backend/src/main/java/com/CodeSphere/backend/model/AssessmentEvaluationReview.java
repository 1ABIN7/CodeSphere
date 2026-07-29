package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/** An immutable evaluator review; the assessment answer stores the consensus. */
@Entity
@Table(name = "assessment_evaluation_reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"answer_id", "evaluator_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentEvaluationReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "answer_id", nullable = false) private Long answerId;
    @Column(name = "evaluator_id", nullable = false) private Long evaluatorId;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal score;
    @Column(columnDefinition = "TEXT") private String feedback;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "rubric_scores", columnDefinition = "jsonb") private Map<String, BigDecimal> rubricScores;
    @Column(name = "reviewed_at", nullable = false) private OffsetDateTime reviewedAt;
}
