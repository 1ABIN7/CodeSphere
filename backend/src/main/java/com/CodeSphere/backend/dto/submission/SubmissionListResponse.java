package com.CodeSphere.backend.dto.submission;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Compact submission representation for history list views.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionListResponse {

    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String status;
    private Integer score;
    private Integer execTime;
    private Integer execMemory;
    private Integer testCasesPassed;
    private Integer totalTestCases;
    private OffsetDateTime createdAt;
}
