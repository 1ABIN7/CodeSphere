package com.CodeSphere.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SectionResultDto {
    private Long sectionId;
    private String title;
    private Double score;
    private Double totalScore;
}
