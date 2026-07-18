package com.codesphere.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamHistoryDTO {
    private Long sessionId;
    private String examName;
    private Double finalScore;
    private String grade;
    private Instant completedAt;
}