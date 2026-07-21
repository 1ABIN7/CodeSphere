package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final AuditLogService auditLogService;

    /**
     * Approves a question for use in assessments and logs the event.
     */
    @PostMapping("/{questionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> approveQuestion(@PathVariable Long questionId, HttpServletRequest request) {
        Long moderatorId = getCurrentUserId();

        questionService.updateStatus(questionId, "APPROVED");

        // [Audit Log] Log Question Approval
        auditLogService.logAction(
                moderatorId,
                "QUESTION-APPROVE",
                "Question ID: " + questionId,
                request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Rejects a question with a reason and logs the event.
     */
    @PostMapping("/{questionId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> rejectQuestion(@PathVariable Long questionId,
                                               @RequestParam String reason,
                                               HttpServletRequest request) {
        Long moderatorId = getCurrentUserId();

        questionService.updateStatus(questionId, "REJECTED");

        // [Audit Log] Log Question Rejection
        auditLogService.logAction(
                moderatorId,
                "QUESTION-REJECT",
                String.format("Question ID: %d | Reason: %s", questionId, reason),
                request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Extracts the current user ID from the Spring Security context.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}