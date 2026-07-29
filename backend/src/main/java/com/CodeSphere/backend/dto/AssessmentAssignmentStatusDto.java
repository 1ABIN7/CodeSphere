package com.CodeSphere.backend.dto;

import java.time.LocalDateTime;

public record AssessmentAssignmentStatusDto(Long userId, String username, String fullName, LocalDateTime deadline,
                                           String sessionStatus, LocalDateTime submittedAt) {}
