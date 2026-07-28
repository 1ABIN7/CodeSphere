package com.CodeSphere.backend.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionHistoryDTO {
    private Long submissionId;
    private Long assessmentId;
    private String assessmentName;
    private Double score;
    private Instant submittedAt;
}