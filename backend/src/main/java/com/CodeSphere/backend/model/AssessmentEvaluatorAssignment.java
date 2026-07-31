package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assessment_evaluator_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"answer_id", "evaluator_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentEvaluatorAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "answer_id", nullable = false) private Long answerId;
    @Column(name = "evaluator_id", nullable = false) private Long evaluatorId;
    @Column(name = "assigned_by", nullable = false) private Long assignedBy;
    @Column(name = "assigned_at", nullable = false) private OffsetDateTime assignedAt;
}
