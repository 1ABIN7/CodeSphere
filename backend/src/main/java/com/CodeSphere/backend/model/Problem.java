package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a coding problem in the problem bank.
 *
 * Maps to the {@code problems} table (V4 + V5 extensions).
 * Each problem has test cases, supports multiple languages,
 * and tracks submission statistics.
 */
@Entity
@Table(name = "problems", indexes = {
    @Index(name = "idx_problems_difficulty", columnList = "difficulty"),
    @Index(name = "idx_problems_published", columnList = "is_published")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(name = "time_limit", nullable = false)
    @Builder.Default
    private Integer timeLimit = 1000; // milliseconds

    @Column(name = "memory_limit", nullable = false)
    @Builder.Default
    private Integer memoryLimit = 262144; // KB (256 MB)

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ---- V5 Extensions ----

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> hints = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String editorial;

    @Column(name = "editorial_code", columnDefinition = "TEXT")
    private String editorialCode;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "acceptance_rate", nullable = false)
    @Builder.Default
    private BigDecimal acceptanceRate = BigDecimal.ZERO;

    @Column(name = "total_submissions", nullable = false)
    @Builder.Default
    private Integer totalSubmissions = 0;

    @Column(name = "accepted_submissions", nullable = false)
    @Builder.Default
    private Integer acceptedSubmissions = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "starter_code", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> starterCode = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "company_tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> companyTags = new ArrayList<>();

    // ---- Relationships ----

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    // ---- Lifecycle ----

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ---- Helper Methods ----

    /**
     * Recalculates the acceptance rate based on current submission counts.
     */
    public void recalculateAcceptanceRate() {
        if (totalSubmissions > 0) {
            this.acceptanceRate = BigDecimal.valueOf(acceptedSubmissions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSubmissions), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.acceptanceRate = BigDecimal.ZERO;
        }
    }

    public void incrementSubmissions(boolean accepted) {
        this.totalSubmissions++;
        if (accepted) {
            this.acceptedSubmissions++;
        }
        recalculateAcceptanceRate();
    }
}
