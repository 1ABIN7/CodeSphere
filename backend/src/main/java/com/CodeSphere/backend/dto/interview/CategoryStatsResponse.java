package com.CodeSphere.backend.dto.interview;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStatsResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryDisplayName;
    private Integer totalAttempted;
    private Integer correctCount;
    private Double accuracy;
    private Integer bestScore;
    private Double avgTimeSeconds;
}
