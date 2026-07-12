package com.CodeSphere.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "question_bank_id", nullable = false)
    private Long questionBankId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(name = "negative_score", nullable = false)
    private Double negativeScore;

    @Column(name = "time_limit_override")
    private Integer timeLimitOverride;
}
