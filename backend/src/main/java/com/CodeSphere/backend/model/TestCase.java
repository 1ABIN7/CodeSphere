package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Represents a single test case for a coding problem.
 *
 * Test cases can be marked as "sample" (visible to candidates)
 * or hidden (used only during judging).
 */
@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "is_sample", nullable = false)
    @Builder.Default
    private Boolean isSample = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ---- V5 Extensions ----

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "time_limit_override")
    private Integer timeLimitOverride;

    @Column(name = "score_weight", nullable = false)
    @Builder.Default
    private Integer scoreWeight = 1;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
