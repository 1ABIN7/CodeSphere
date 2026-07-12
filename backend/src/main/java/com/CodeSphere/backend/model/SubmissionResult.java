package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Per-test-case execution result within a submission.
 *
 * Stores the actual output, execution time/memory, and verdict
 * for each individual test case run by the judge engine.
 */
@Entity
@Table(name = "submission_results", indexes = {
    @Index(name = "idx_subresults_submission", columnList = "submission_id"),
    @Index(name = "idx_subresults_testcase", columnList = "test_case_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "exec_time")
    private Integer execTime; // milliseconds

    @Column(name = "exec_memory")
    private Integer execMemory; // KB

    @Column(name = "error_output", columnDefinition = "TEXT")
    private String errorOutput;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
