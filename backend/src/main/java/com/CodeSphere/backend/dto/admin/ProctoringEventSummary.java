package com.CodeSphere.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.Map;

public record ProctoringEventSummary(
        Long id,
        Long sessionId,
        Long assessmentId,
        String candidateName,
        String eventType,
        String severity,
        LocalDateTime timestamp,
        Map<String, Object> metadata
) { }
