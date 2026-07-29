package com.CodeSphere.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResultDTO {
    private Long sessionId;
    private Long userId;
    private Long assessmentId;
    private Double score;
    private Double totalScore;
    private String status;
    private String assessmentTitle;
    private Double passingScore;
    private Boolean passed;
    private List<String> evaluatorFeedback;
    private List<SectionResultDto> sections;
}
