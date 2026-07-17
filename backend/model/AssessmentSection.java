package com.codesphere.backend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_sections")
public class AssessmentSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "section_type")
    private String sectionType; // e.g., "MCQ", "WRITTEN", "COMPREHENSION"

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // null if section doesn't have an isolated timer

    @Enumerated(EnumType.STRING)
    @Column(name = "navigation_mode")
    private NavigationMode navigationMode = NavigationMode.FREE;

    public enum NavigationMode {
        FREE, SEQUENTIAL
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public String getSectionType() { return sectionType; }
    public void setSectionType(String sectionType) { this.sectionType = sectionType; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public NavigationMode getNavigationMode() { return navigationMode; }
    public void setNavigationMode(NavigationMode navigationMode) { this.navigationMode = navigationMode; }
}