package com.CodeSphere.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "section_type")
    private String sectionType;

    @Column(name = "navigation_mode", nullable = false)
    private String navigationMode; // SEQUENTIAL, FREE
}