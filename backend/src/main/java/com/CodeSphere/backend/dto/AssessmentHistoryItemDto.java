package com.CodeSphere.backend.dto;

import java.time.OffsetDateTime;

public record AssessmentHistoryItemDto(Long assessmentId, String title, Double score, Double totalScore,
                                       String status, OffsetDateTime submittedAt) {}
