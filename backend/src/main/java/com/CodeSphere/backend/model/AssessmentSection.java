package com.CodeSphere.backend.model;

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

    private Long assessmentId;
    private Integer sectionOrder;

    private String title;
    private String sectionType;

    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    private NavigationMode navigationMode;

    public enum NavigationMode {
        FREE, SEQUENTIAL
    }

    // --- Helper / Alias methods for backward compatibility ---
    public Integer getTimeLimitMinutes() {
        return this.durationMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.durationMinutes = timeLimitMinutes;
    }
}