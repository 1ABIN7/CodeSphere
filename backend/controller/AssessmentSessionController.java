package com.codesphere.backend.controller;

import com.codesphere.backend.dto.AssessmentResultDTO;
import com.codesphere.backend.dto.SubmissionDTO;
import com.codesphere.backend.service.AssessmentService;
import com.codesphere.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
@RequiredArgsConstructor
public class AssessmentSessionController {

    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;

    /**
     * Initiates a live assessment session for the authenticated user and logs the critical action.
     */
    @PostMapping("/{assessmentId}/start")
    public ResponseEntity<Void> startAssessmentSession(@PathVariable Long assessmentId, HttpServletRequest request) {
        Long userId = getCurrentUserId();

        assessmentService.start(assessmentId, userId);

        // [Audit Log] Log live user attempt initialization
        auditLogService.logAction(
                userId,
                "ASSESSMENT-START",
                "Assessment ID: " + assessmentId,
                request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Submits an active assessment session, processes scores, and logs the critical submission action.
     */
    @PostMapping("/{assessmentId}/submit")
    public ResponseEntity<AssessmentResultDTO> submitAssessmentSession(@PathVariable Long assessmentId,
                                                                       @RequestBody SubmissionDTO submission,
                                                                       HttpServletRequest request) {
        Long userId = getCurrentUserId();

        AssessmentResultDTO result = (AssessmentResultDTO) assessmentService.submit(assessmentId, userId, submission);

        // [Audit Log] Log critical test completion state change
        auditLogService.logAction(
                userId,
                "ASSESSMENT-SUBMIT",
                "Assessment ID: " + assessmentId,
                request
        );

        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}