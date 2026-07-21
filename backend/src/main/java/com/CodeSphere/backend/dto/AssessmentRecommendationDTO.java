package com.CodeSphere.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRecommendationDTO {
    private Long assessmentId;
    private String title;
    private String recommendationReason;
    private String difficulty;
}